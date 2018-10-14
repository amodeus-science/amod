package amod.demo.dispatcher.carpooling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.PseudoRemovePriorityQueue;

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
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class CarPooling2Dispatcher extends SharedPartitionedDispatcher {

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
    private XZOSelector xZOSelector;
    private XDOSelector xDOSelector;
    private PZOSelector pZOSelector;
    private PSOSelector pSOSelector;
    private final int timeStep;
    private final int planningHorizon;
    private final int fixedCarCapacity;
    private final AVRouter router;
    private List<Link> linkList;

    protected CarPooling2Dispatcher(Config config, //
            AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            AVRouter router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher, //
            TravelData travelData, //
            MatsimAmodeusDatabase db) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork, db);
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
        this.timeStep = 5;
        // dispatchPeriod = safeConfig.getInteger("dispatchPeriod", timeStep *
        // 60);
        dispatchPeriod = timeStep * 60;
        this.planningHorizon = 8;
        this.fixedCarCapacity = 2;
        this.router = router;
        this.linkList = ICRApoolingDispatcherUtils.getLinkforStation(network, config, virtualNetwork);

    }

    @Override
    protected void redispatch(double now) {

        final long round_now = Math.round(now);

        List<List<Coord>> dataList = CarPooling2DispatcherUtils.getDemand(network, timeStep, config, virtualNetwork);
        Link destination = CarPooling2DispatcherUtils.getLinkforStation(network, dataList, virtualNetwork.getVirtualNode(0));

        

        if (round_now % dispatchPeriod == 0 && round_now >= dispatchPeriod) {

            // travel times
            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorForVirtualNetwork
                    .computeTravelTimes(virtualNetwork.getVirtualLinks(), Quantity.of(now, SI.SECOND), router, linkList);

            double[][] StationsRoadGraph = CarPooling2DispatcherUtils.getVirtualNetworkForMatlab(virtualNetwork);
            double[][] TravelTimesStations = CarPooling2DispatcherUtils
                    .getTravelTimesVirtualNetworkForMatlab(virtualNetwork, timeStep, travelTimes);

            List<double[][]> FlowsOut = CarPooling2DispatcherUtils.getFlowsOut(network, virtualNetwork, planningHorizon,
                    timeStep, config, round_now);

            List<AVRequest> requests = getUnassignedAVRequests();
            double[][] pastUnassignedRequests = new double[virtualNetwork.getvNodesCount()][virtualNetwork
                    .getvNodesCount()];
            for (AVRequest req : requests) {
                int fromLink = virtualNetwork.getVirtualNode(req.getFromLink()).getIndex();
                int toLink = virtualNetwork.getVirtualNode(req.getToLink()).getIndex();
                pastUnassignedRequests[fromLink][toLink] = pastUnassignedRequests[fromLink][toLink] + 1;
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
                    xZOSelector = new XZOSelector(xZOControlLaw);
                    xDOSelector = new XDOSelector(xDOControlLaw);
                    pZOSelector = new PZOSelector(pZOControlLaw);
                    pSOSelector = new PSOSelector(pSOControlLaw);

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
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOControlPolicy = pZOSelector.getPZOCommands(
                        virtualNetwork, StayRoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if (pZOControlPolicy != null) {
                    for (Triple<RoboTaxi, AVRequest, AVRequest> triple : pZOControlPolicy) {
                        RoboTaxi roboTaxi = triple.getLeft();
                        if (!roboTaxi.getMenu().getCourses().isEmpty() && roboTaxi.getMenu().getCourses().size() == 1
                                && roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                            roboTaxi.getMenu().clearWholeMenu();
                        }
                        AVRequest avRequest1 = triple.getMiddle();
                        AVRequest avRequest2 = triple.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest1);
                        addSharedRoboTaxiPickup(roboTaxi, avRequest2);
                        SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
                        roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
                        GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 4);
                        GlobalAssert.that(roboTaxi.checkMenuConsistency());

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // pSO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Pair<RoboTaxi, AVRequest>> pSOControlPolicy = pSOSelector.getPSOCommands(virtualNetwork,
                        soRoboTaxi, virtualNodeAVFromRequests, virtualNodeAVToRequests);
                if (pSOControlPolicy != null) {
                    for (Pair<RoboTaxi, AVRequest> pair : pSOControlPolicy) {
                        RoboTaxi roboTaxi = pair.getLeft();
                        if (roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                            roboTaxi.getMenu().removeAVCourse(0);
                        }
                        AVRequest avRequest2 = pair.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest2);
                        SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
                        roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
                        GlobalAssert.that(roboTaxi.getMenu().getCourses().size() == 3);
                        GlobalAssert.that(roboTaxi.checkMenuConsistency());

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // xZO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchPeriod
                && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, Link>> xZOControlPolicy = xZOSelector.getXZOCommands(virtualNetwork,
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

        // xDO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Pair<RoboTaxi, Link>> xDOControlPolicy = xDOSelector.getXDOCommands(virtualNetwork, soRoboTaxi,
                        virtualNodeAVFromRequests, virtualNodeAVToRequests);
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
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayWithoutCustomerRoboTaxi();
            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
                try {
                    List<Pair<RoboTaxi, Link>> controlPolicy = rebalanceSelector.getRebalanceCommands(fromNode,
                            stayRoboTaxi, virtualNetwork, linkList);
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
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60)) {
            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
                for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
                    List<List<double[]>> controlLawXZO = xZOSelector.getControlLawXZO();
                    List<List<double[]>> controlLawPZO = pZOSelector.getControlLawPZO();
                    List<List<double[]>> controlLawPSO = pSOSelector.getControlLawPSO();
                    double[] xZOqueueFromTo = controlLawXZO.get(toNode.getIndex()).get(fromNode.getIndex());
                    double[] pZOqueueFromTo = controlLawPZO.get(fromNode.getIndex()).get(toNode.getIndex());
                    List<double[]> pSOqueueFrom = controlLawPSO.get(fromNode.getIndex());
                    DoublePredicate predicate = d -> d == (toNode.getIndex() + 1);
                    List<double[]> presence = pSOqueueFrom.stream()
                            .filter(q -> Arrays.stream(q).anyMatch(predicate) == true).collect(Collectors.toList());

                    if (Arrays.stream(xZOqueueFromTo).sum() == 0 && Arrays.stream(pZOqueueFromTo).sum() == 0
                            && presence.isEmpty()) {
                        Map<VirtualNode<Link>, List<RoboTaxi>> freeCarsMap = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
                        List<RoboTaxi> freeCarsAt = freeCarsMap.get(fromNode);
                        List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
                        List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
                        List<AVRequest> fromToRequests = fromRequests.stream().filter(req -> toRequests.contains(req))
                                .collect(Collectors.toList());
                        if (!freeCarsAt.isEmpty() && !fromToRequests.isEmpty()) {
                            for (AVRequest avRequest : fromToRequests) {
                                RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest,
                                        freeCarsAt);
                                if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
                                        .getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                                    closestRoboTaxi.getMenu().removeAVCourse(0);
                                }
                                if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
                                        .getCourses().get(0).getMealType() == SharedMealType.DROPOFF) {
                                    addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
                                    SharedCourse sharedAVCourse = SharedCourse.pickupCourse(avRequest);
                                    closestRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
                                    GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 3);
                                    GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
                                } else {
                                    addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
                                    GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 2);
                                    GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
                                }
                                freeCarsAt.remove(closestRoboTaxi);
                                freeCarsMap.get(fromNode).remove(closestRoboTaxi);
                                if (freeCarsAt.isEmpty()) {
                                    break;
                                }
                            }

                        }
                    }

                }
            }

        }

        if (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)) {
            List<List<double[]>> controlLawXZO = xZOSelector.getControlLawXZO();
            List<List<double[]>> controlLawXDO = xDOSelector.getControlLawXDO();
            List<List<double[]>> controlLawPZO = pZOSelector.getControlLawPZO();
            List<List<double[]>> controlLawPSO = pSOSelector.getControlLawPSO();
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
                        System.out.println("NOT All commands used");
                    }

                    numberXdo = numberXdo + Arrays.stream(xdoQueue).sum();

                    if (Arrays.stream(pzoQueue).sum() != 0) {
                        List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
                        List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
                        List<RoboTaxi> freecar = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi().get(fromNode);
                        System.out.println("NOT All commands used");
                    }

                    numberPzo = numberPzo + Arrays.stream(pzoQueue).sum();
                    numberPso = numberPso + Arrays.stream(psoQueue).sum();

                }
            }

            if (numberRebalance == 0) {
                System.out.println("all rebalance commands used");
            } else {
                System.out.println("NOT ALL rebalance commands used");
            }

            if (numberXzo == 0) {
                System.out.println("all XZO commands used");
            } else {
                System.out.println("NOT ALL XZO commands used");
            }

            if (numberXdo == 0) {
                System.out.println("all XDO commands used");
            } else {
                System.out.println("NOT ALL XDO commands used");
            }

            if (numberPzo == 0) {
                System.out.println("all PZO commands used");
            } else {
                System.out.println("NOT ALL PZO commands used");
            }

            if (numberPso == 0) {
                System.out.println("all PSO commands used");
            } else {
                System.out.println("NOT ALL PSO commands used");
            }
        }

        // Assign closest car
        // if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >=
        // dispatchTime
        // && round_now < (dispatchTime + timeStep * 60)) {
        // for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
        // for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
        // List<List<double[]>> controlLawXZO = xZOSelector.getControlLawXZO();
        // List<List<double[]>> controlLawPZO = pZOSelector.getControlLawPZO();
        // List<List<double[]>> controlLawPSO = pSOSelector.getControlLawPSO();
        // double[] xZOqueueFromTo =
        // controlLawXZO.get(toNode.getIndex()).get(fromNode.getIndex());
        // double[] pZOqueueFromTo =
        // controlLawPZO.get(fromNode.getIndex()).get(toNode.getIndex());
        // List<double[]> pSOqueueFrom = controlLawPSO.get(fromNode.getIndex());
        // DoublePredicate predicate = d -> d == (toNode.getIndex() + 1);
        // List<double[]> presence = pSOqueueFrom.stream()
        // .filter(q -> Arrays.stream(q).anyMatch(predicate) ==
        // true).collect(Collectors.toList());
        // Collection<RoboTaxi> roboTaxis = getRoboTaxisAvailable();
        // List<AVRequest> fromRequests =
        // getVirtualNodeFromAVRequest().get(fromNode);
        // List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
        // List<AVRequest> fromToRequests = fromRequests.stream().filter(req ->
        // toRequests.contains(req))
        // .collect(Collectors.toList());
        //
        // if (!fromToRequests.isEmpty() && !roboTaxis.isEmpty()) {
        // for (AVRequest avRequest : fromToRequests) {
        // RoboTaxi closestRoboTaxi =
        // StaticHelperCarPooling.findClostestVehicle(avRequest, roboTaxis);
        //
        // if (closestRoboTaxi.getMenu().getCourses().isEmpty()
        // || (closestRoboTaxi.getMenu().getCourses().size() == 1 &&
        // closestRoboTaxi.getMenu()
        // .getStarterCourse().getMealType() == SharedMealType.REDIRECT)) {
        // if (Arrays.stream(pZOqueueFromTo).sum() != 0) {
        // for (int ipzo = 0; ipzo < pZOqueueFromTo.length; ipzo++) {
        // if (pZOqueueFromTo[ipzo] != 0) {
        // int index = (int) (pZOqueueFromTo[ipzo] - 1);
        // List<AVRequest> toRequests2 = getVirtualNodeToAVRequest()
        // .get(index);
        // List<AVRequest> fromToRequests2 = fromRequests.stream()
        // .filter(req -> toRequests2.contains(req))
        // .collect(Collectors.toList());
        // if (fromToRequests2.isEmpty() && fromRequests.size() < 2) {
        // continue;
        // }
        //
        // if (!closestRoboTaxi.getMenu().getCourses().isEmpty()) {
        // closestRoboTaxi.getMenu().clearWholeMenu();
        // }
        //
        // addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
        // getVirtualNodeFromAVRequest().get(fromNode).remove(avRequest);
        // getVirtualNodeToAVRequest().get(toNode).remove(avRequest);
        //
        // AVRequest avRequestSecond = StaticHelperCarPooling
        // .findClostestRequestfromRequest(avRequest, fromToRequests2);
        //
        // addSharedRoboTaxiPickup(closestRoboTaxi, avRequestSecond);
        // SharedCourse sharedAVCourse2 =
        // SharedCourse.pickupCourse(avRequestSecond);
        // closestRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
        //
        // roboTaxis.remove(closestRoboTaxi);
        //
        // GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() ==
        // 4);
        // GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
        // pZOSelector.removePZOCommand(fromNode, toNode, ipzo);
        // }
        //
        // }
        //
        // }
        // if (Arrays.stream(xZOqueueFromTo).sum() != 0
        // && closestRoboTaxi.getMenu().getCourses().isEmpty()) {
        // for (int ixzo = 0; ixzo < xZOqueueFromTo.length; ixzo++) {
        // if (xZOqueueFromTo[ixzo] != 0) {
        //
        // if (!closestRoboTaxi.getMenu().getCourses().isEmpty()) {
        // closestRoboTaxi.getMenu().clearWholeMenu();
        // }
        //
        // addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
        // VirtualNode<Link> toNodeRedirect = virtualNetwork
        // .getVirtualNode((int) xZOqueueFromTo[ixzo] - 1);
        // Set<Link> linkSet = toNodeRedirect.getLinks();
        // List<Link> linkList = new ArrayList<Link>(linkSet);
        // List<Link> linkListFiltered = linkList.stream().filter(link -> link
        // != null)
        // .collect(Collectors.toList());
        // Link redirectLink = linkListFiltered
        // .get(new Random().nextInt(linkListFiltered.size() - 1));
        // SharedCourse redirectCourse =
        // SharedCourse.redirectCourse(redirectLink, //
        // Double.toString(now) + closestRoboTaxi.getId().toString());
        // addSharedRoboTaxiRedirect(closestRoboTaxi, redirectCourse);
        // closestRoboTaxi.getMenu().moveAVCourseToPrev(redirectCourse);
        //
        // roboTaxis.remove(closestRoboTaxi);
        //
        // GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() ==
        // 3);
        // GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
        // xZOSelector.removeXZOCommand(fromNode, toNode, ixzo);
        // }
        // }
        // }
        // }
        //
        // if (closestRoboTaxi.getCurrentNumberOfCustomersOnBoard() == 1) {
        // if (!presence.isEmpty()) {
        // double[] pSOqueueFromTo = presence.get(0);
        // for (int ipso = 0; ipso < pSOqueueFromTo.length; ipso++) {
        // if (pSOqueueFromTo[ipso] == toNode.getIndex() + 1) {
        // if (closestRoboTaxi.getMenu().getCourses().get(0)
        // .getMealType() == SharedMealType.REDIRECT) {
        // closestRoboTaxi.getMenu().removeAVCourse(0);
        // }
        // addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
        // SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest);
        // closestRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);
        //
        // roboTaxis.remove(closestRoboTaxi);
        //
        // GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() ==
        // 3);
        // GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
        // int toNodeIndex = pSOqueueFrom.indexOf(pSOqueueFromTo);
        // pSOSelector.removePSOCommand(fromNode, toNodeIndex, ipso);
        //
        // }
        // }
        // }
        //
        // }
        //
        // }
        // }
        // }
        // }
        // }

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

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayOrSingleOrRebalanceRoboTaxi() {
        List<RoboTaxi> taxiList = getRoboTaxis().stream()
                .filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
                        && car.getMenu().getCourses().isEmpty())
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation()))
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF)
                        || (car.getMenu().getCourses().size() == 2
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation())))
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

    protected final Collection<RoboTaxi> getRoboTaxisAvailable() {
        return getRoboTaxis().stream() //
                .filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
                        && car.getMenu().getCourses().isEmpty())
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation()))
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF)
                        || (car.getMenu().getCourses().size() == 2
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation()))) //
                .collect(Collectors.toList());
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

        @Inject
        private MatsimAmodeusDatabase db;
        
        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(
                    EuclideanDistanceFunction.INSTANCE);

            return new CarPooling2Dispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager,
                    network, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelData, db);
        }
    }

}
