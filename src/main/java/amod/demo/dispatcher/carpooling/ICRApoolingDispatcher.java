package amod.demo.dispatcher.carpooling;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.amodeus.dispatcher.core.SharedPartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedCourse;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractRoboTaxiDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.GlobalBipartiteMatching;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.traveldata.TravelData;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.router.AVRouter;

public class ICRApoolingDispatcher extends SharedPartitionedDispatcher {

	private final int dispatchPeriod;
	private final int rebalancingPeriod;
	private final AbstractVirtualNodeDest virtualNodeDest;
	private final AbstractRoboTaxiDestMatcher vehicleDestMatcher;
	private final int nVNodes;
	private final int nVLinks;
	private final Network network;
	private final DistanceFunction distanceFunction;
	private final DistanceHeuristics distanceHeuristics;
	private Tensor printVals = Tensors.empty();
	private TravelData travelData;
	private final Config config;
	private double dispatchTime;
	private RebalanceCarSelector rebalanceSelector;
	private XZOControl xZOControl;
	private XSOControl xSOControl;
	private PZOControl pZOControl;
	private PSOControl pSOControl;
	private final int timeStep;
	private final int planningHorizon;
	private final int fixedCarCapacity;
	private final AVRouter router;
	private LinkWait linkWait;
	static private final Logger logger = Logger.getLogger(ICRApoolingDispatcher.class);

	protected ICRApoolingDispatcher(Config config, //
			AVDispatcherConfig avconfig, //
			AVGeneratorConfig generatorConfig, //
			TravelTime travelTime, //
			AVRouter router, //
			EventsManager eventsManager, //
			Network network, //
			VirtualNetwork<Link> virtualNetwork, //
			AbstractVirtualNodeDest abstractVirtualNodeDest, //
			AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher, //
			TravelData travelData) {
		super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
		virtualNodeDest = abstractVirtualNodeDest;
		vehicleDestMatcher = abstractVehicleDestMatcher;
		this.travelData = travelData;
		this.network = network;
		nVNodes = virtualNetwork.getvNodesCount();
		nVLinks = virtualNetwork.getvLinksCount();
		SafeConfig safeConfig = SafeConfig.wrap(avconfig);
		rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 30);
		distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
				DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
		System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
		this.distanceFunction = distanceHeuristics.getDistanceFunction(network);
		this.config = config;
		this.timeStep = 10;
		// dispatchPeriod = safeConfig.getInteger("dispatchPeriod", timeStep *
		// 60);
		dispatchPeriod = timeStep * 60;
		this.planningHorizon = 30;
		this.fixedCarCapacity = 2;
		this.router = router;

	}

	@Override
	protected void redispatch(double now) {

		final long round_now = Math.round(now);

		if (round_now % dispatchPeriod == 0 && round_now >= dispatchPeriod) {

			// travel times
			Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorForVirtualNetwork
					.computeTravelTimes(virtualNetwork.getVirtualLinks(), Quantity.of(now, SI.SECOND), router);

			double[][] StationsRoadGraph = CarPooling2DispatcherUtils.getVirtualNetworkForMatlab(virtualNetwork);
			double[][] TravelTimesStations = CarPooling2DispatcherUtils
					.getTravelTimesVirtualNetworkForMatlab(virtualNetwork, timeStep, travelTimes);

			linkWait = new LinkWait(new HashMap<VirtualNode<Link>, List<Link>>());
			HashMap<VirtualNode<Link>, List<Link>> linkMap = linkWait.getLinkWait();
			Pair<List<double[][]>, HashMap<VirtualNode<Link>, List<Link>>> FlowsOutpair = ICRApoolingDispatcherUtils
					.getFlowsOut(network, virtualNetwork, planningHorizon, timeStep, config, round_now, linkMap);
			List<double[][]> FlowsOut = FlowsOutpair.getLeft();
			linkMap = FlowsOutpair.getRight();
			linkWait.setLinkWait(linkMap);

			List<AVRequest> requests = getUnassignedAVRequests();
			double[][] pastUnassignedRequests = new double[virtualNetwork.getvNodesCount()][virtualNetwork
					.getvNodesCount()];
			for (AVRequest req : requests) {
				int fromLink = virtualNetwork.getVirtualNode(req.getFromLink()).getIndex();
				int toLink = virtualNetwork.getVirtualNode(req.getToLink()).getIndex();
				pastUnassignedRequests[fromLink][toLink] = pastUnassignedRequests[fromLink][toLink] + 1;
				linkWait.addLinkWaitElement(virtualNetwork.getVirtualNode(req.getToLink()), req.getFromLink());
			}

			for (int i = 0; i < virtualNetwork.getvNodesCount(); i++) {
				for (int j = 0; j < virtualNetwork.getvNodesCount(); j++) {
					FlowsOut.get(0)[i][j] = FlowsOut.get(0)[i][j] + pastUnassignedRequests[i][j];
				}
			}

			Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayRoboTaxi();
			Map<VirtualNode<Link>, List<RoboTaxi>> rebalancingTaxi = getDestinationVirtualNodeRedirectOnlyRoboTaxi();
			Map<VirtualNode<Link>, List<RoboTaxi>> doRoboTaxi = getDestinationVirtualNodeDORoboTaxiOnlyDropoff();
			Collection<RoboTaxi> oneCustomerRoboTaxi = getRoboTaxisWithNumberOfCustomer(1);
			Collection<RoboTaxi> twoCustomerRoboTaxi = getRoboTaxisWithNumberOfCustomer(fixedCarCapacity);

			double[][] rState = CarPooling2DispatcherUtils.getRState(round_now, planningHorizon, timeStep,
					fixedCarCapacity, stayRoboTaxi, rebalancingTaxi, oneCustomerRoboTaxi, twoCustomerRoboTaxi,
					virtualNetwork, travelTimes, router);

			Map<VirtualNode<Link>, List<RoboTaxi>> soFromNode = getVirtualNodeSORedirectRoboTaxi();

			List<double[][]> xState = CarPooling2DispatcherUtils.getXState(round_now, planningHorizon, timeStep,
					fixedCarCapacity, soFromNode, doRoboTaxi, virtualNetwork, router);

			try {
				// initialize server
				JavaContainerSocket javaContainerSocket = new JavaContainerSocket(
						new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

				{ // add inputs to server
					Container container = new Container("Network");

					// add network to container
					double[] networkNode = new double[StationsRoadGraph.length];
					for (int index = 0; index < StationsRoadGraph.length; ++index) {
						networkNode = StationsRoadGraph[index];
						container.add((new DoubleArray("roadGraph" + index, new int[] { StationsRoadGraph.length },
								networkNode)));
					}

					// add travel times to container
					double[] travelTimeskNode = new double[TravelTimesStations.length];
					for (int index = 0; index < TravelTimesStations.length; ++index) {
						travelTimeskNode = TravelTimesStations[index];
						container.add((new DoubleArray("travelTimes" + index, new int[] { TravelTimesStations.length },
								travelTimeskNode)));
					}

					// add r_state cars to container
					double[] rStateAt = new double[rState.length];
					int indexCar = 0;
					for (double[] CarsAtTime : rState) {
						indexCar = indexCar + 1;
						rStateAt = CarsAtTime;
						container.add((new DoubleArray("rState" + indexCar, new int[] { rStateAt.length }, rStateAt)));
					}

					int xindex = 0;
					for (double[][] xs : xState) {
						double[] x = new double[xs.length];
						for (int index = 0; index < xs.length; ++index) {
							x = xs[index];
							container.add((new DoubleArray("xstate" + xindex + 0 + index, new int[] { x.length }, x)));
						}
						xindex = xindex + 1;
					}

					int flowIndex = 0;
					for (double[][] flows : FlowsOut) {
						double[] flowsOutAt = new double[flows.length];
						for (int index = 0; index < flows.length; ++index) {
							flowsOutAt = flows[index];
							container.add((new DoubleArray("flowsOut" + flowIndex + 0 + index,
									new int[] { flows.length }, flowsOutAt)));
						}
						flowIndex = flowIndex + 1;
					}

					// add planning horizon to container
					double[] PlanningHorizonDouble = new double[] { planningHorizon };
					container.add((new DoubleArray("PlanningHorizon", new int[] { 1 }, PlanningHorizonDouble)));

					System.out.println("Sending to server");
					javaContainerSocket.writeContainer(container);

				}

				{ // get outputs from server
					System.out.println("Waiting for server");
					Container container = javaContainerSocket.blocking_getContainer();
					// System.out.println("received: " + container);

					// get control inputs for rebalancing from container
					List<double[]> rebalanceControlLaw = new ArrayList<>();
					for (int i = 1; i <= virtualNetwork.getVirtualNodes().size(); ++i) {
						rebalanceControlLaw.add(CarPooling2DispatcherUtils.getArray(container, "rState" + i));
					}

					List<List<double[]>> xZOControlLaw = new ArrayList<>();
					for (int i = 1; i <= virtualNetwork.getVirtualNodes().size(); ++i) {
						List<double[]> xZOConrtol = new ArrayList<>();
						for (int j = 1; j <= virtualNetwork.getVirtualNodes().size(); j++) {
							xZOConrtol.add((j - 1),
									CarPooling2DispatcherUtils.getArray(container, "xzoState" + i + 0 + j));
						}
						xZOControlLaw.add((i - 1), xZOConrtol);
					}

					List<List<double[]>> xDOControlLaw = new ArrayList<>();
					for (int i = 1; i <= virtualNetwork.getVirtualNodes().size(); ++i) {
						List<double[]> xDOConrtol = new ArrayList<>();
						for (int j = 1; j <= virtualNetwork.getVirtualNodes().size(); j++) {
							xDOConrtol.add((j - 1),
									CarPooling2DispatcherUtils.getArray(container, "xsoState" + i + 0 + j));
						}
						xDOControlLaw.add((i - 1), xDOConrtol);

					}

					List<List<double[]>> pZOControlLaw = new ArrayList<>();
					for (int i = 1; i <= virtualNetwork.getVirtualNodes().size(); ++i) {
						List<double[]> pZOConrtol = new ArrayList<>();
						for (int j = 1; j <= virtualNetwork.getVirtualNodes().size(); j++) {
							pZOConrtol.add((j - 1),
									CarPooling2DispatcherUtils.getArray(container, "pzoState" + i + 0 + j));
						}
						pZOControlLaw.add((i - 1), pZOConrtol);

					}

					List<List<double[]>> pSOControlLaw = new ArrayList<>();
					for (int i = 1; i <= virtualNetwork.getVirtualNodes().size(); ++i) {
						List<double[]> pSOConrtol = new ArrayList<>();
						for (int j = 1; j <= virtualNetwork.getVirtualNodes().size(); j++) {
							pSOConrtol.add((j - 1),
									CarPooling2DispatcherUtils.getArray(container, "psoState" + i + 0 + j));
						}
						pSOControlLaw.add((i - 1), pSOConrtol);

					}

					rebalanceSelector = new RebalanceCarSelector(rebalanceControlLaw);
					xZOControl = new XZOControl(xZOControlLaw);
					xSOControl = new XSOControl(xDOControlLaw);
					pZOControl = new PZOControl(pZOControlLaw, linkWait.getLinkWait());
					pSOControl = new PSOControl(pSOControlLaw, linkWait.getLinkWait());

					dispatchTime = round_now;

				}

				javaContainerSocket.close();
				System.out.println("finished");
			} catch (Exception exception) {
				exception.printStackTrace();
				throw new RuntimeException(); // dispatcher will not work if
												// constructor has issues
			}
		}

		// pZO cars
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
			Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
			Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
			try {
				List<Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>>> pZOControlPolicy = pZOControl
						.getPZOCommands(virtualNetwork, StayRoboTaxi, VirtualNodeAVFromRequests,
								VirtualNodeAVToRequests);
				linkWait.setLinkWait(pZOControl.getLinkMapPZO());
				if (pZOControlPolicy != null) {
					for (Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> triple : pZOControlPolicy) {
						RoboTaxi roboTaxi = triple.getLeft();
						if (!roboTaxi.getMenu().getCourses().isEmpty() && roboTaxi.getMenu().getCourses().size() == 1
								&& roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
							roboTaxi.getMenu().clearWholeMenu();
						}
						AVRequest avRequest1 = triple.getMiddle().getLeft();
						AVRequest avRequest2 = triple.getMiddle().getRight();
						Link waitingLink = triple.getRight().getLeft();
						VirtualNode<Link> waitingCustomerDirection = triple.getRight().getRight();

						if (avRequest1 != null && avRequest2 != null) {
							addSharedRoboTaxiPickup(roboTaxi, avRequest1);
							addSharedRoboTaxiPickup(roboTaxi, avRequest2);
							SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
							roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 4);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						} else if (avRequest1 != null && avRequest2 == null) {
							addSharedRoboTaxiPickup(roboTaxi, avRequest1);
							SharedCourse waitingCourse = SharedCourse.waitingCourse(waitingLink,
									waitingCustomerDirection.getId().toString() + "-" + Double.toString(now)
											+ roboTaxi.getId().toString());
							addSharedRoboTaxiWaiting(roboTaxi, waitingCourse);
							roboTaxi.getMenu().moveAVCourseToPrev(waitingCourse);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						} else if (avRequest1 == null && avRequest2 != null) {
							addSharedRoboTaxiPickup(roboTaxi, avRequest2);
							SharedCourse waitingCourse = SharedCourse.waitingCourse(waitingLink,
									waitingCustomerDirection.getId().toString() + "-" + Double.toString(now)
											+ roboTaxi.getId().toString());
							addSharedRoboTaxiWaiting(roboTaxi, waitingCourse);
							roboTaxi.getMenu().moveAVCourseToPrev(waitingCourse);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						}

					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// waiting cars
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> waitingToboTaxi = getVirtualNodeWaitingRoboTaxi();
			Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
			Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
			for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
				List<RoboTaxi> availableRoboTaxi = waitingToboTaxi.get(fromNode);
				if (availableRoboTaxi.isEmpty()) {
					continue;
				}
				List<AVRequest> fromRequests = VirtualNodeAVFromRequests.get(fromNode);
				if (fromRequests.isEmpty()) {
					continue;
				}
				for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
					List<AVRequest> toRequests = VirtualNodeAVToRequests.get(toNode);
					if (toRequests.isEmpty()) {
						continue;
					}
					List<AVRequest> requests = fromRequests.stream().filter(req -> toRequests.contains(req))
							.collect(Collectors.toList());

					for (AVRequest req : requests) {
						String toVirtualNodeID = toNode.getId();
						if (availableRoboTaxi.isEmpty()) {
							break;
						}
						List<RoboTaxi> roboTaxiList = availableRoboTaxi.stream()
								.filter(car -> car.getMenu().getStarterCourse().getRequestId().toString().split("-")[0]
										.equals(toVirtualNodeID))
								.collect(Collectors.toList());
						if (roboTaxiList.isEmpty()) {
							break;
						}
						RoboTaxi roboTaxi = StaticHelperCarPooling.findClostestVehicle(req, roboTaxiList);
						GlobalAssert.that(
								roboTaxi.getMenu().getStarterCourse().getMealType() == SharedMealType.WAITFORCUSTOMER);
						roboTaxi.getMenu().removeAVCourse(0);
						addSharedRoboTaxiPickup(roboTaxi, req);
						SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(req);
						roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
						GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
						GlobalAssert.that(roboTaxi.checkMenuConsistency());

						availableRoboTaxi.remove(roboTaxi);
						waitingToboTaxi.get(fromNode).remove(roboTaxi);

					}
				}
			}
		}

		// pSO cars
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
			try {
				List<Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>>> pSOControlPolicy = pSOControl
						.getPSOCommands(virtualNetwork, soRoboTaxi, virtualNodeAVFromRequests, virtualNodeAVToRequests);
				linkWait.setLinkWait(pSOControl.getLinkMapPSO());
				if (pSOControlPolicy != null) {
					for (Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>> triple : pSOControlPolicy) {
						RoboTaxi roboTaxi = triple.getLeft();
						if (roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
							roboTaxi.getMenu().removeAVCourse(0);
						}
						AVRequest avRequest2 = triple.getMiddle();

						if (avRequest2 != null) {
							addSharedRoboTaxiPickup(roboTaxi, avRequest2);
							SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
							roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						} else if (avRequest2 == null) {
							Link waitingLink = triple.getRight().getLeft();
							VirtualNode<Link> waitingCustomerDirection = triple.getRight().getRight();
							SharedCourse waitingCourse = SharedCourse.waitingCourse(waitingLink,
									waitingCustomerDirection.getId().toString() + "-" + Double.toString(now)
											+ roboTaxi.getId().toString());
							addSharedRoboTaxiWaiting(roboTaxi, waitingCourse);
							roboTaxi.getMenu().moveAVCourseToPrev(waitingCourse);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 2);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						}
					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// xZO cars
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
			try {
				List<Triple<RoboTaxi, AVRequest, Link>> xZOControlPolicy = xZOControl.getXZOCommands(virtualNetwork,
						stayRoboTaxi, virtualNodeAVFromRequests, virtualNodeAVToRequests);
				if (xZOControlPolicy != null) {
					for (Triple<RoboTaxi, AVRequest, Link> triple : xZOControlPolicy) {
						RoboTaxi roboTaxi = triple.getLeft();
						if (!roboTaxi.getMenu().getCourses().isEmpty() && roboTaxi.getMenu().getCourses().size() == 1
								&& roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
							roboTaxi.getMenu().clearWholeMenu();
						}
						AVRequest avRequest = triple.getMiddle();
						Link redirectLink = triple.getRight();
						addSharedRoboTaxiPickup(roboTaxi, avRequest);
						if (virtualNetwork.getVirtualNode(avRequest.getToLink()) == virtualNetwork
								.getVirtualNode(redirectLink)) {
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 2);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
							continue;
						}
						if (virtualNetwork.getVirtualNode(roboTaxi.getDivertableLocation()) == virtualNetwork
								.getVirtualNode(redirectLink)) {
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 2);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
							continue;
						}
						SharedCourse redirectCourse = SharedCourse.redirectCourse(redirectLink, //
								Double.toString(now) + roboTaxi.getId().toString());
						addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
						roboTaxi.getMenu().moveAVCourseToPrev(redirectCourse);
						GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
						GlobalAssert.that(roboTaxi.checkMenuConsistency());

					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// xSO cars
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
			try {
				List<Pair<RoboTaxi, Link>> xDOControlPolicy = xSOControl.getXSOCommands(virtualNetwork, soRoboTaxi);
				if (xDOControlPolicy != null) {
					for (Pair<RoboTaxi, Link> pair : xDOControlPolicy) {
						RoboTaxi roboTaxi = pair.getLeft();
						if (roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
							roboTaxi.getMenu().removeAVCourse(0);
						}
						Link redirectLink = pair.getRight();
						if (virtualNetwork.getVirtualNode(roboTaxi.getCurrentDriveDestination()) == virtualNetwork
								.getVirtualNode(redirectLink)) {
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 1);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
							continue;
						}

						if (virtualNetwork.getVirtualNode(roboTaxi.getDivertableLocation()) == virtualNetwork
								.getVirtualNode(redirectLink)) {
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 1);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
							continue;
						}
						SharedCourse redirectCourse = SharedCourse.redirectCourse(redirectLink, //
								Double.toString(now) + roboTaxi.getId().toString());
						addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
						roboTaxi.getMenu().moveAVCourseToPrev(redirectCourse);
						GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 2);
						GlobalAssert.that(roboTaxi.checkMenuConsistency());

					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Rebalancing
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayWithoutCustomerRoboTaxi();
			for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
				try {
					List<Pair<RoboTaxi, Link>> controlPolicy = rebalanceSelector.getRebalanceCommands(fromNode,
							stayRoboTaxi, virtualNetwork);
					if (controlPolicy != null) {
						for (Pair<RoboTaxi, Link> pair : controlPolicy) {
							RoboTaxi roboTaxi = pair.getLeft();
							Link redirectLink = pair.getRight();
							// setRoboTaxiRebalance(pair.getLeft(),
							// pair.getRight());

							if (virtualNetwork.getVirtualNode(roboTaxi.getDivertableLocation()) == virtualNetwork
									.getVirtualNode(redirectLink)) {
								GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 0);
								GlobalAssert.that(roboTaxi.checkMenuConsistency());
								continue;
							}
							SharedCourse redirectCourse = SharedCourse.redirectCourse(redirectLink, //
									Double.toString(now) + roboTaxi.getId().toString());
							addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
							GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 1);
							GlobalAssert.that(roboTaxi.checkMenuConsistency());
						}

					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Assign unassigned requests
//		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
//				&& round_now < (dispatchTime + timeStep * 60))
//				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
//			for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
//				for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
//					List<List<double[]>> controlLawXZO = xZOControl.getControlLawXZO();
//					List<List<double[]>> controlLawPZO = pZOControl.getControlLawPZO();
//					List<List<double[]>> controlLawPSO = pSOControl.getControlLawPSO();
//					double[] xZOqueueFromTo = controlLawXZO.get(toNode.getIndex()).get(fromNode.getIndex());
//					double[] pZOqueueFromTo = controlLawPZO.get(fromNode.getIndex()).get(toNode.getIndex());
//					List<double[]> pSOqueueFrom = controlLawPSO.get(fromNode.getIndex());
//					DoublePredicate predicate = d -> d == (toNode.getIndex() + 1);
//					List<double[]> presence = pSOqueueFrom.stream()
//							.filter(q -> Arrays.stream(q).anyMatch(predicate) == true).collect(Collectors.toList());
//
//					if (Arrays.stream(xZOqueueFromTo).sum() == 0 && Arrays.stream(pZOqueueFromTo).sum() == 0
//							&& presence.isEmpty()) {
//						List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
//						List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
//						List<AVRequest> fromToRequests = fromRequests.stream().filter(req -> toRequests.contains(req))
//								.collect(Collectors.toList());
//						if (!fromToRequests.isEmpty()) {
//							for (AVRequest avRequest : fromToRequests) {
//								Collection<RoboTaxi> availableCars = getRoboTaxisAvailable(avRequest);
//								if (availableCars.isEmpty()) {
//									continue;
//								}
//								RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest,
//										availableCars);
//								if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
//										.getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
//									closestRoboTaxi.getMenu().removeAVCourse(0);
//									GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//								}
//
//								if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
//										.getCourses().get(0).getMealType() == SharedMealType.WAITFORCUSTOMER) {
//									closestRoboTaxi.getMenu().removeAVCourse(0);
//									GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 1);
//									GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//								}
//
//								if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
//										.getCourses().get(0).getMealType() == SharedMealType.DROPOFF) {
//									addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
//									SharedCourse sharedAVCourse = SharedCourse.pickupCourse(avRequest);
//									closestRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
//									GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 3);
//									GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//								} else {
//									addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
//									GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 2);
//									GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//								}
//							}
//
//						}
//					}
//
//				}
//			}
//
//		}
		
		if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
				&& round_now < (dispatchTime + timeStep * 60))
				|| (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
			
			Collection<RoboTaxi> doRoboTaxis = getRoboTaxisWithNumberOfCustomer(2);
			Collection<RoboTaxi> soRoboTaxis = getRoboTaxisWithNumberOfCustomer(1);
			Collection<RoboTaxi> emptyRoboTaxis = getRoboTaxisWithNumberOfCustomer(0);
			System.out.println("Number of DO Cars: " + doRoboTaxis.size());
			System.out.println("Number of SO Cars: " + soRoboTaxis.size());
			System.out.println("Number of empty Cars: " + emptyRoboTaxis.size());
		}

		// check if control inputs used
		if (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)) {
			List<List<double[]>> controlLawXZO = xZOControl.getControlLawXZO();
			List<List<double[]>> controlLawXDO = xSOControl.getControlLawXSO();
			List<List<double[]>> controlLawPZO = pZOControl.getControlLawPZO();
			List<List<double[]>> controlLawPSO = pSOControl.getControlLawPSO();
			List<double[]> controlLawRebalance = rebalanceSelector.getControlLawRebalance();
			double numberRebalance = 0;
			double numberXzo = 0;
			double numberXdo = 0;
			double numberPzo = 0;
			double numberPso = 0;
			for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
				double[] rebalancequeue = controlLawRebalance.get(fromNode.getIndex());
				numberRebalance = numberRebalance + Arrays.stream(rebalancequeue).sum();
				for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
					double[] xzoQueue = controlLawXZO.get(toNode.getIndex()).get(fromNode.getIndex());
					double[] xdoQueue = controlLawXDO.get(toNode.getIndex()).get(fromNode.getIndex());
					double[] pzoQueue = controlLawPZO.get(fromNode.getIndex()).get(toNode.getIndex());
					double[] psoQueue = controlLawPSO.get(fromNode.getIndex()).get(toNode.getIndex());
					numberXzo = numberXzo + Arrays.stream(xzoQueue).sum();

					if (Arrays.stream(xzoQueue).sum() != 0) {
						List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
						List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
						List<RoboTaxi> freecar = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi().get(fromNode);
						List<AVRequest> fromToRequest = fromRequests.stream().filter(req -> toRequests.contains(req))
								.collect(Collectors.toList());
						GlobalAssert.that(freecar.isEmpty() || fromToRequest.isEmpty());
					}

					numberXdo = numberXdo + Arrays.stream(xdoQueue).sum();

					if (Arrays.stream(xdoQueue).sum() != 0) {
						List<RoboTaxi> freecar = getVirtualNodeSORoboTaxi().get(fromNode).stream()
								.filter(car -> (car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF
										&& toNode.getLinks().contains(car.getCurrentDriveDestination())
										|| (car.getMenu().getStarterCourse().getMealType() == SharedMealType.REDIRECT
												&& toNode.getLinks()
														.contains(car.getMenu().getCourses().get(1).getLink()))))
								.collect(Collectors.toList());
						GlobalAssert.that(freecar.isEmpty());
					}

					numberPzo = numberPzo + Arrays.stream(pzoQueue).sum();

					if (Arrays.stream(pzoQueue).sum() != 0) {
						List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
						List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
						List<AVRequest> fromToRequest = fromRequests.stream().filter(req -> toRequests.contains(req))
								.collect(Collectors.toList());
						List<RoboTaxi> freecar = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi().get(fromNode);
						GlobalAssert.that(freecar.isEmpty() || fromToRequest.isEmpty());
						for (int i = 0; i < pzoQueue.length; i++) {
							int index = (int) pzoQueue[i] - 1;
							if (index < 0) {
								continue;
							}
							List<AVRequest> fromToRequest2 = fromRequests
									.stream().filter(req -> getVirtualNodeToAVRequest()
											.get(virtualNetwork.getVirtualNode(index)).contains(req))
									.collect(Collectors.toList());
							GlobalAssert.that(freecar.isEmpty() || fromToRequest2.isEmpty());
							GlobalAssert.that(freecar.isEmpty() || fromToRequest.isEmpty() || fromToRequest2.isEmpty());
						}
					}
					numberPso = numberPso + Arrays.stream(psoQueue).sum();

					if (Arrays.stream(psoQueue).sum() != 0) {
						List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
						List<RoboTaxi> freecar = getVirtualNodeSORoboTaxi().get(fromNode).stream()
								.filter(car -> (car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF
										&& toNode.getLinks().contains(car.getCurrentDriveDestination())
										|| (car.getMenu().getStarterCourse().getMealType() == SharedMealType.REDIRECT
												&& toNode.getLinks()
														.contains(car.getMenu().getCourses().get(1).getLink()))))
								.collect(Collectors.toList());
						for (int i = 0; i < psoQueue.length; i++) {
							int index = (int) psoQueue[i] - 1;
							if (index < 0) {
								continue;
							}
							List<AVRequest> fromToRequest2 = fromRequests
									.stream().filter(req -> getVirtualNodeToAVRequest()
											.get(virtualNetwork.getVirtualNode(index)).contains(req))
									.collect(Collectors.toList());
							GlobalAssert.that(freecar.isEmpty() || fromToRequest2.isEmpty());
						}
						GlobalAssert.that(freecar.isEmpty() || fromRequests.isEmpty());
					}
				}
			}

			if (numberRebalance == 0) {
				System.out.println("all rebalance commands used");
			} else {
				logger.warn("NOT ALL rebalance commands used");
			}

			if (numberXzo == 0) {
				System.out.println("all XZO commands used");
			} else {
				logger.warn("NOT ALL XZO commands used");
			}

			if (numberXdo == 0) {
				System.out.println("all XSO commands used");
			} else {
				logger.warn("NOT ALL XSO commands used");
			}

			if (numberPzo == 0) {
				System.out.println("all PZO commands used");
			} else {
				logger.warn("NOT ALL PZO commands used");
			}

			if (numberPso == 0) {
				System.out.println("all PSO commands used");
			} else {
				logger.warn("NOT ALL PSO commands used");
			}

			List<AVRequest> unassignedRequests = getUnassignedAVRequests();
			if (!unassignedRequests.isEmpty()) {
				logger.warn("Open Requests");
				System.out.println("Number of open requests: " + unassignedRequests.size());
			}
		}

		// Assign last unassigned requests
//		if (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)
//				&& !getUnassignedAVRequests().isEmpty()) {
//			for (AVRequest avRequest : getUnassignedAVRequests()) {
//				Collection<RoboTaxi> availableCars = getRoboTaxisAvailable(avRequest);
//				if (availableCars.isEmpty()) {
//					continue;
//				}
//				RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
//				if (!closestRoboTaxi.getMenu().getCourses().isEmpty()
//						&& closestRoboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
//					closestRoboTaxi.getMenu().removeAVCourse(0);
//					GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//				}
//
//				if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu().getCourses().get(0)
//						.getMealType() == SharedMealType.WAITFORCUSTOMER) {
//					closestRoboTaxi.getMenu().removeAVCourse(0);
//					GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 1);
//					GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//				}
//
//				if (!closestRoboTaxi.getMenu().getCourses().isEmpty()
//						&& closestRoboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.DROPOFF) {
//					addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
//					SharedCourse sharedAVCourse = SharedCourse.pickupCourse(avRequest);
//					closestRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
//					GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 3);
//					GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//				} else {
//					addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
//					GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 2);
//					GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
//				}
//			}
//			if (!getUnassignedAVRequests().isEmpty()) {
//				logger.warn("Open Requests");
//			}
//		}

	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayRoboTaxi() {
		return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(RoboTaxiStatus.STAY), RoboTaxi::getDivertableLocation);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayWithoutCustomerRoboTaxi() {
		List<RoboTaxi> taxiList = getDivertableUnassignedRoboTaxis().stream().filter(car -> car.isInStayTask()
				&& car.getCurrentNumberOfCustomersOnBoard() == 0 && car.getMenu().getCourses().isEmpty())
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(taxiList, RoboTaxi::getDivertableLocation);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi() {
		List<RoboTaxi> taxiList = getRoboTaxis().stream()
				.filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
						&& car.getMenu().getCourses().isEmpty())
						|| (car.getMenu().getCourses().size() == 1
								&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
								&& virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
										.getVirtualNode(car.getDivertableLocation())))
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(taxiList, RoboTaxi::getDivertableLocation);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeWaitingRoboTaxi() {
		List<RoboTaxi> taxiList = getRoboTaxis().stream()
				.filter(car -> !car.getMenu().getCourses().isEmpty()
						&& car.getMenu().getStarterCourse().getMealType() == SharedMealType.WAITFORCUSTOMER)
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(taxiList, RoboTaxi::getDivertableLocation);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getDestinationVirtualNodeRedirectOnlyRoboTaxi() {
		List<RoboTaxi> rebalancingTaxi = getRoboTaxisWithNumberOfCustomer(0).stream()
				.filter(car -> car.getMenu().getCourses().size() == 1
						&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT)
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(rebalancingTaxi, RoboTaxi::getCurrentDriveDestination);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeSORoboTaxi() {
		List<RoboTaxi> soFiltered = getRoboTaxisWithNumberOfCustomer(1).stream()
				.filter(car -> (car.getMenu().getCourses().size() == 1
						&& car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF)
						|| (car.getMenu().getCourses().size() == 2
								&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
								&& virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
										.getVirtualNode(car.getDivertableLocation())))
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(soFiltered, RoboTaxi::getDivertableLocation);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeSORedirectRoboTaxi() {
		List<RoboTaxi> soFiltered = getRoboTaxisWithNumberOfCustomer(1).stream()
				.filter(car -> (car.getMenu().getCourses().size() == 2
						&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT))
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(soFiltered, RoboTaxi::getCurrentDriveDestination);
	}

	private Map<VirtualNode<Link>, List<RoboTaxi>> getDestinationVirtualNodeDORoboTaxiOnlyDropoff() {
		List<RoboTaxi> doFiltered = getRoboTaxisWithNumberOfCustomer(2).stream()
				.filter(car -> car.getMenu().getCourses().size() == 2 && car.getMenu().getCourses().get(0)
						.getMealType() == car.getMenu().getCourses().get(1).getMealType())
				.collect(Collectors.toList());
		return virtualNetwork.binToVirtualNode(doFiltered, RoboTaxi::getCurrentDriveDestination);
	}

	private Map<VirtualNode<Link>, List<AVRequest>> getVirtualNodeFromAVRequest() {
		return virtualNetwork.binToVirtualNode(getUnassignedAVRequests(), AVRequest::getFromLink);
	}

	private Map<VirtualNode<Link>, List<AVRequest>> getVirtualNodeToAVRequest() {
		return virtualNetwork.binToVirtualNode(getUnassignedAVRequests(), AVRequest::getToLink);
	}

	protected final Collection<RoboTaxi> getRoboTaxisWithNumberOfCustomer(int x) {
		return getDivertableRoboTaxis().stream() //
				.filter(rt -> rt.getCurrentNumberOfCustomersOnBoard() == x) //
				.collect(Collectors.toList());
	}

	protected final Collection<RoboTaxi> getRoboTaxisAvailable(AVRequest avRequest) {
		VirtualNode<Link> toVirtualNode = virtualNetwork.getVirtualNode(avRequest.getToLink());
		List<RoboTaxi> availableCars = getRoboTaxis().stream() //
				.filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
						&& car.getMenu().getCourses().isEmpty())
						|| (car.getMenu().getCourses().size() == 1
								&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
								&& virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
										.getVirtualNode(car.getDivertableLocation()))
						|| (!car.getMenu().getCourses().isEmpty() && car.getMenu().getStarterCourse().getMealType() == SharedMealType.WAITFORCUSTOMER
								&& car.getMenu().getStarterCourse().getRequestId().toString().split("-")[0]
										.equals(toVirtualNode.getId()))
						|| (car.getMenu().getCourses().size() == 1
								&& car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF
								&& toVirtualNode.getLinks().contains(car.getCurrentDriveDestination()))
						|| (car.getMenu().getCourses().size() == 2
								&& car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
								&& virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
										.getVirtualNode(car.getDivertableLocation())
								&& toVirtualNode.getLinks().contains(car.getMenu().getCourses().get(1).getLink()))) //
				.collect(Collectors.toList());
		return availableCars;
	}

	public static class Factory implements AVDispatcherFactory {
		@Inject
		@Named(AVModule.AV_MODE)
		private TravelTime travelTime;

		@Inject
		private EventsManager eventsManager;

		@Inject(optional = true)
		private TravelData travelData;

		@Inject
		@Named(AVModule.AV_MODE)
		private Network network;

		@Inject(optional = true)
		private VirtualNetwork<Link> virtualNetwork;

		@Inject
		private Config config;

		@Override
		public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
			AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

			AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
			AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(
					EuclideanDistanceFunction.INSTANCE);

			return new ICRApoolingDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager,
					network, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelData);
		}
	}

}
