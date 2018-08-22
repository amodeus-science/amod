package amod.demo.dispatcher;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import amod.demo.dispatcher.claudioForDejan.TravelTimeCalculatorClaudioForDejan;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlanEntry;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.amodeus.dispatcher.core.SharedPartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedAVGeneratorConfig;
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
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.dispatcher.AVDispatcher.AVDispatcherFactory;
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
//        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", timeStep * 60);
        dispatchPeriod = timeStep*60;
        this.planningHorizon = 8;
        this.fixedCarCapacity = 2;

    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0 && round_now>=dispatchPeriod) {
            // travel times
            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorForVirtualNetwork
                    .computeTravelTimes(virtualNetwork.getVirtualLinks());

            double[][] StationsRoadGraph = CarPooling2DispatcherUtils.getVirtualNetworkForMatlab(virtualNetwork);
            double[][] TravelTimesStations = CarPooling2DispatcherUtils
                    .getTravelTimesVirtualNetworkForMatlab(virtualNetwork, timeStep, travelTimes);

            List<double[][]> FlowsOut = CarPooling2DispatcherUtils.getFlowsOut(network, virtualNetwork, planningHorizon,
                    timeStep, config, round_now);

            List<RoboTaxi> taxiWithCustomer = getRoboTaxiSubset(RoboTaxiStatus.DRIVEWITHCUSTOMER);
            List<RoboTaxi> taxiRebalancing = getRoboTaxiSubset(RoboTaxiStatus.REBALANCEDRIVE);
            Map<VirtualNode<Link>, List<RoboTaxi>> rebalancingTaxi = getVirtualNodeDestinationRebalancingRoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayRoboTaxi();
            
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
            Collection<AVRequest> avRequests = getAVRequests();

            double[][] rState = CarPooling2DispatcherUtils.getRState(round_now, planningHorizon, timeStep, avRequests,
                    fixedCarCapacity, stayRoboTaxi, taxiWithCustomer, rebalancingTaxi, virtualNetwork, travelTimes);
            List<double[][]> xState = CarPooling2DispatcherUtils.getXState(round_now, planningHorizon, timeStep,
                    avRequests, fixedCarCapacity, soRoboTaxi, taxiWithCustomer, virtualNetwork);

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
                            container.add((new DoubleArray("xstate" + xindex + index, new int[] { x.length }, x)));
                        }
                        xindex = xindex +1;
                    }

                    int flowIndex = 0;
                    for (double[][] flows : FlowsOut) {
                        double[] flowsOutAt = new double[flows.length];
                        for (int index = 0; index < flows.length; ++index) {
                            flowsOutAt = flows[index];
                            container
                                    .add((new DoubleArray("flowsOut" + flowIndex + index, new int[] { flows.length }, flowsOutAt)));
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
                    System.out.println("received: " + container);
                    
                 // get control inputs for rebalancing from container
                  List<double[]> rebalanceControlLaw = new ArrayList<>();        
                  for(int i=1; i<=virtualNetwork.getVirtualNodes().size(); ++i) {
                      rebalanceControlLaw.add(CarPooling2DispatcherUtils.getArray(container, "rState"+i));
                  }

                  
                  List<List<double[]>> xZOControlLaw = new ArrayList<>();
                  for(int i=1; i<=virtualNetwork.getVirtualNodes().size(); ++i) {
                      List<double[]> xZOConrtol = new ArrayList<>();
                      for(int j=1; j<=virtualNetwork.getVirtualNodes().size(); j++) {
                          xZOConrtol.add((j-1),CarPooling2DispatcherUtils.getArray(container, "xzoState"+i+j));
                      }
                      xZOControlLaw.add((i-1), xZOConrtol);
                  }
                  
                  
                  List<List<double[]>> xDOControlLaw = new ArrayList<>();
                  for(int i=1; i<=virtualNetwork.getVirtualNodes().size(); ++i) {
                      List<double[]> xDOConrtol = new ArrayList<>();
                      for(int j=1; j<=virtualNetwork.getVirtualNodes().size(); j++) {
                          xDOConrtol.add((j-1),CarPooling2DispatcherUtils.getArray(container, "xzoState"+i+j));
                      }
                      xDOControlLaw.add((i-1), xDOConrtol);
                      
                  }
                  
                  List<List<double[]>> pZOControlLaw = new ArrayList<>();
                  for(int i=1; i<=virtualNetwork.getVirtualNodes().size(); ++i) {
                      List<double[]> pZOConrtol = new ArrayList<>();
                      for(int j=1; j<=virtualNetwork.getVirtualNodes().size(); j++) {
                          pZOConrtol.add((j-1),CarPooling2DispatcherUtils.getArray(container, "xzoState"+i+j));
                      }
                      pZOControlLaw.add((i-1), pZOConrtol);
                      
                  }
                  
                  List<List<double[]>> pSOControlLaw = new ArrayList<>();
                  for(int i=1; i<=virtualNetwork.getVirtualNodes().size(); ++i) {
                      List<double[]> pSOConrtol = new ArrayList<>();
                      for(int j=1; j<=virtualNetwork.getVirtualNodes().size(); j++) {
                          pSOConrtol.add((j-1),CarPooling2DispatcherUtils.getArray(container, "xzoState"+i+j));
                      }
                      pSOControlLaw.add((i-1), pSOConrtol);
                      
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

        // Rebalancing
//        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime && round_now < (dispatchTime + timeStep * 60)) {
//            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayRoboTaxi();
//            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
//                try {
//                    List<Pair<RoboTaxi, Link>> controlPolicy = rebalanceSelector.getRebalanceCommands(fromNode,
//                            stayRoboTaxi, virtualNetwork);
//                    if (controlPolicy != null) {
//                        for (Pair<RoboTaxi, Link> pair : controlPolicy) {
//                            setRoboTaxiRebalance(pair.getLeft(), pair.getRight());
//                        }
//
//                    }
//                } catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }

        // xZO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchPeriod && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, Link>> xZOControlPolicy = xZOSelector.getXZOCommands(virtualNetwork,
                        stayRoboTaxi, virtualNodeAVFromRequests, virtualNodeAVToRequests);
                if (xZOControlPolicy != null) {
                    for (Triple<RoboTaxi, AVRequest, Link> triple : xZOControlPolicy) {
                        RoboTaxi roboTaxi = triple.getLeft();
                        AVRequest avRequest = triple.getMiddle();
                        Link redirectLink = triple.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest);
                        SharedCourse redirectCourse = SharedCourse.redirectCourse(redirectLink, //
                                Double.toString(now) + roboTaxi.getId().toString());
                        addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
                        roboTaxi.getMenu().moveAVCourseToPrev(redirectCourse);

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // xDO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();

            try {
                List<Pair<RoboTaxi, Link>> xDOControlPolicy = xDOSelector.getXDOCommands(virtualNetwork, soRoboTaxi, virtualNodeAVFromRequests, virtualNodeAVToRequests);
                if (xDOControlPolicy != null) {
                    for (Pair<RoboTaxi, Link> pair : xDOControlPolicy) {
                        RoboTaxi roboTaxi = pair.getLeft();
                        Link redirectLink = pair.getRight();
                        SharedCourse redirectCourse = SharedCourse.redirectCourse(redirectLink, //
                                Double.toString(now) + roboTaxi.getId().toString());
                        addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
                        roboTaxi.getMenu().moveAVCourseToPrev(redirectCourse);

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        // pZO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOControlPolicy = pZOSelector.getPZOCommands(
                        virtualNetwork, StayRoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if (pZOControlPolicy != null) {
                    for (Triple<RoboTaxi, AVRequest, AVRequest> triple : pZOControlPolicy) {
                        RoboTaxi roboTaxi = triple.getLeft();
                        AVRequest avRequest1 = triple.getMiddle();
                        AVRequest avRequest2 = triple.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest1);
                        addSharedRoboTaxiPickup(roboTaxi, avRequest2);
                        SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
                        roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // pSO cars
        if (round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime && round_now < (dispatchTime + timeStep * 60)) {
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();

            try {
                List<Pair<RoboTaxi, AVRequest>> pSOControlPolicy = pSOSelector.getPSOCommands(virtualNetwork,
                        SORoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if (pSOControlPolicy != null) {
                    for (Pair<RoboTaxi, AVRequest> pair : pSOControlPolicy) {
                        RoboTaxi roboTaxi = pair.getLeft();
                        AVRequest avRequest2 = pair.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest2);
                        SharedCourse sharedAVCourse2 = SharedCourse.pickupCourse(avRequest2);
                        roboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse2);

                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeDivertableUnassignedRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getDivertableUnassignedRoboTaxis(), RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeOneCustomerRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxisWithAtLeastXFreeSeats(1), RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeOWithoutCustomerRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getDivertableRoboTaxisWithoutCustomerOnBoard(),
                RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeDestinationRebalancingRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(RoboTaxiStatus.REBALANCEDRIVE),
                RoboTaxi::getCurrentDriveDestination);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(RoboTaxiStatus.STAY), RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeSORoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxisWithNumberOfCustomer(1), RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeDORoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxisWithNumberOfCustomer(2), RoboTaxi::getDivertableLocation);
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
                    new EuclideanDistanceFunction());

            return new CarPooling2Dispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager,
                    network, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelData);
        }
    }

}
