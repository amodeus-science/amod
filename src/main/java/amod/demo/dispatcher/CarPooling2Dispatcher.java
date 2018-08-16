package amod.demo.dispatcher;

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
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedAVCourse;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedAVMealType;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedAVMenu;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedDispatcherExample;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.HungarBiPartVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.traveldata.TravelData;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
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
    private final AbstractVehicleDestMatcher vehicleDestMatcher;
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
    private PZOSelector pZOSelector;
    private PSOSelector pSOSelector;
    private final int timeStep;

    protected CarPooling2Dispatcher(Config config, //
            AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            AVRouter router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            TravelData travelData) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        this.travelData = travelData;
        this.network = network;
        nVNodes = virtualNetwork.getvNodesCount();
        nVLinks = virtualNetwork.getvLinksCount();
        SafeConfig safeConfig = SafeConfig.wrap(avconfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 300);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 30);
        distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
                DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
        System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
        this.distanceFunction = distanceHeuristics.getDistanceFunction(network);
        this.config = config;
        this.timeStep = 10;

    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {
            
            // travel times
            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorForVirtualNetwork.computeTravelTimes(virtualNetwork.getVirtualLinks());
            
            double[][] StationsRoadGraph = CarPooling2DispatcherUtils.getVirtualNetworkForMatlab(virtualNetwork);
            double[][] TravelTimesStations = CarPooling2DispatcherUtils.getTravelTimesVirtualNetworkForMatlab(virtualNetwork, timeStep, travelTimes);
            int PlanningHorizon = 50;
            int fixedCarCapacity = 2;
            
            List<double[][]> FlowsOut = CarPooling2DispatcherUtils.getFlowsOut(network, virtualNetwork, PlanningHorizon, timeStep, config, round_now);
            
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> RebalanceRoboTaxi = getVirtualNodeRebalancingRoboTaxi(); 
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi = getVirtualNodeDORoboTaxi();
            
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
                        
            double[][] test = FlowsOut.get(49);
            
            for(int i=0;i<test.length;i++) {
                for(int j=0;j<test.length;j++) {
                    System.out.println(test[i][j]);
                }
                
            }
            
            for (RoboTaxi sharedRoboTaxi : getDivertableUnassignedRoboTaxis()) {
                if (getUnassignedAVRequests().size() >= 4) {

                    AVRequest firstRequest = getUnassignedAVRequests().get(0);
                    AVRequest secondRequest = getUnassignedAVRequests().get(1);
                    AVRequest thirdRequest = getUnassignedAVRequests().get(2);

                    addSharedRoboTaxiPickup(sharedRoboTaxi, firstRequest);

                    addSharedRoboTaxiPickup(sharedRoboTaxi, secondRequest);
                    SharedAVCourse sharedAVCourse = new SharedAVCourse(secondRequest.getId(), SharedAVMealType.PICKUP);
                    sharedRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
                    
                    addSharedRoboTaxiPickup(sharedRoboTaxi, thirdRequest);
                    SharedAVCourse sharedAVCourse3 = new SharedAVCourse(thirdRequest.getId(), SharedAVMealType.PICKUP);
                    sharedRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse3);
                    sharedRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse3);


                    sharedRoboTaxi.checkMenuConsistency();
                } else {
                    break;
                }
            }
            
            List<double[]> rebalanceControlLaw = new ArrayList<>();
            List<List<double[]>> xZOControlLaw = new ArrayList<>();
            rebalanceSelector = new RebalanceCarSelector(rebalanceControlLaw);
            xZOSelector = new XZOSelector(xZOControlLaw);
            
            dispatchTime = round_now;
        }
        
        // Rebalancing
        if(round_now % 10 == 0 && round_now >= dispatchTime && round_now < (dispatchTime+5*60) ) {
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            for(VirtualNode<Link> fromNode: virtualNetwork.getVirtualNodes()) {
                try {
                    List<Pair<RoboTaxi, Link>> controlPolicy = rebalanceSelector.getRebalanceCommands(fromNode, StayRoboTaxi, virtualNetwork);
                    if(controlPolicy != null) {
                        for(Pair<RoboTaxi, Link> pair: controlPolicy) {
                            setRoboTaxiRebalance(pair.getLeft(), pair.getRight());
                        }
                        
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }  
        }
        
        // xZO cars
        if(round_now % 10 == 0 && round_now >= dispatchTime && round_now < (dispatchTime+5*60) ) {
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, Link>> xZOControlPolicy = xZOSelector.getXZOCommands(virtualNetwork, StayRoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if(xZOControlPolicy != null) {
                    for(Triple<RoboTaxi, AVRequest, Link> triple: xZOControlPolicy) {
                        RoboTaxi taxi = triple.getLeft();
                        addSharedRoboTaxiPickup(taxi, triple.getMiddle());
                        addSharedRoboTaxiPickup(triple.getLeft(), null);
                        SharedAVCourse sharedAVCourse = new SharedAVCourse(null, SharedAVMealType.PICKUP);
                        taxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
                        
                    }
                    
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        // pZO cars
        if(round_now % 10 == 0 && round_now >= dispatchTime && round_now < (dispatchTime+5*60) ) {
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            try {
                List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOControlPolicy = pZOSelector.getPZOCommands(virtualNetwork, StayRoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if(pZOSelector != null) {
                    for(Triple<RoboTaxi, AVRequest, AVRequest> triple: pZOControlPolicy) {
                        RoboTaxi taxi = triple.getLeft();
                        addSharedRoboTaxiPickup(taxi, triple.getMiddle());
                        addSharedRoboTaxiPickup(taxi, triple.getRight());
                        SharedAVCourse sharedAVCourse = new SharedAVCourse(triple.getRight().getId(), SharedAVMealType.PICKUP);
                        taxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
                        
                    }
                    
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        // pSO cars
        if(round_now % 10 == 0 && round_now >= dispatchTime && round_now < (dispatchTime+5*60) ) {
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            
            try {
                List<Pair<RoboTaxi, AVRequest>> pSOControlPolicy = pSOSelector.getPSOCommands(virtualNetwork, SORoboTaxi, VirtualNodeAVFromRequests, VirtualNodeAVToRequests);
                if(pZOSelector != null) {
                    for(Pair<RoboTaxi, AVRequest> pair: pSOControlPolicy) {
                        RoboTaxi taxi = pair.getLeft();
                        addSharedRoboTaxiPickup(taxi, pair.getRight());
                        SharedAVCourse sharedAVCourse = new SharedAVCourse(pair.getRight().getId(), SharedAVMealType.PICKUP);
                        taxi.getMenu().moveAVCourseToPrev(sharedAVCourse);
                        
                    }
                    
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        if (round_now % dispatchPeriod == 0) {
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi = getVirtualNodeDORoboTaxi();
            Collection<RoboTaxi> three = getRoboTaxisWithNumberOfCustomer(3);
            VirtualNode<Link> node = virtualNetwork.getVirtualNode(7);
            if(!SORoboTaxi.values().isEmpty()) {
                if(!SORoboTaxi.get(node).isEmpty()) {
                    RoboTaxi taxi = SORoboTaxi.get(node).get(0);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().beginTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().endTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().size());
                    System.out.println(taxi.getId());
                    for(RoboTaxiPlanEntry plans: taxi.getCurrentPlans(round_now).getPlans().values()) {
                        System.out.println(plans.beginTime);
                        System.out.println(plans.endTime);
                    }
   
                    System.out.println("finished");
                }
            }
            
            if(!DORoboTaxi.values().isEmpty()) {
                if(!DORoboTaxi.get(node).isEmpty()) {
                    RoboTaxi taxi = DORoboTaxi.get(node).get(0);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().beginTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().endTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().size());
                    for(RoboTaxiPlanEntry plans: taxi.getCurrentPlans(round_now).getPlans().values()) {
                        System.out.println(plans.beginTime);
                        System.out.println(plans.endTime);
                    }
                    System.out.println(taxi.getCapacity());
                    System.out.println(taxi.getCurrentNumberOfCustomersOnBoard());
                    System.out.println(taxi.getId());
                    Id<Request> req = taxi.getMenu().getCourses().get(0).getRequestId();
                    SharedAVCourse deki = taxi.getMenu().getCourses().get(0).dropoffCourse(req);
                    System.out.println("finished");
                }
            }
            
            if(!three.isEmpty()) {
                if(!three.isEmpty()) {
                    RoboTaxi taxi = three.iterator().next();
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().beginTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().endTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().size());
                    for(RoboTaxiPlanEntry plans: taxi.getCurrentPlans(round_now).getPlans().values()) {
                        System.out.println(plans.beginTime);
                        System.out.println(plans.endTime);
                    }
                    System.out.println(taxi.getCapacity());
                    System.out.println(taxi.getCurrentNumberOfCustomersOnBoard());
                    System.out.println(taxi.getId());
                    System.out.println("finished");
                }
            }
            
            
            System.out.println("finisthed");
        }

    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeDivertableUnassignedRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getDivertableUnassignedRoboTaxis(), RoboTaxi::getDivertableLocation);
    }
    
    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeOneCustomerRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxisWithAtLeastXFreeSeats(1), RoboTaxi::getDivertableLocation);
    }
    
    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeOWithoutCustomerRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getDivertableRoboTaxisWithoutCustomerOnBoard(), RoboTaxi::getDivertableLocation);
    }
    
    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeRebalancingRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(RoboTaxiStatus.REBALANCEDRIVE), RoboTaxi::getDivertableLocation);
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
                .filter(rt -> rt.getCurrentNumberOfCustomersOnBoard()==x) //
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
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());

            return new CarPooling2Dispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher, travelData);
        }
    }

}

