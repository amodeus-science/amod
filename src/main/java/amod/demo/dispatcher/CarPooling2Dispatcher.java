package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {

            // travel times
            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorForVirtualNetwork.computeTravelTimes(virtualNetwork.getVirtualLinks());
            
            double[][] StationsRoadGraph = CarPooling2DispatcherUtils.getVirtualNetworkForMatlab(virtualNetwork);
            double[][] TravelTimesStations = CarPooling2DispatcherUtils.getTravelTimesVirtualNetworkForMatlab(virtualNetwork, travelTimes);
            int PlanningHorizon = 50;
            int fixedCarCapacity = 2;
            
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> RebalanceRoboTaxi = getVirtualNodeRebalancingRoboTaxi(); 
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi = getVirtualNodeDORoboTaxi();
            
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests = getVirtualNodeToAVRequest();
            
            for (RoboTaxi sharedRoboTaxi : getDivertableUnassignedRoboTaxis()) {
                if (getUnassignedAVRequests().size() >= 4) {

                    AVRequest firstRequest = getUnassignedAVRequests().get(0);
                    AVRequest secondRequest = getUnassignedAVRequests().get(1);

                    addSharedRoboTaxiPickup(sharedRoboTaxi, firstRequest);

                    addSharedRoboTaxiPickup(sharedRoboTaxi, secondRequest);
                    SharedAVCourse sharedAVCourse = new SharedAVCourse(secondRequest.getId(), SharedAVMealType.PICKUP);
                    sharedRoboTaxi.getMenu().moveAVCourseToPrev(sharedAVCourse);


                    sharedRoboTaxi.checkMenuConsistency();
                } else {
                    break;
                }
            }
            
        }
        
        if (round_now % dispatchPeriod == 0) {
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi = getVirtualNodeSORoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi = getVirtualNodeDORoboTaxi();
            VirtualNode<Link> node = virtualNetwork.getVirtualNode(7);
            if(!SORoboTaxi.values().isEmpty()) {
                if(!SORoboTaxi.get(node).isEmpty()) {
                    RoboTaxi taxi = SORoboTaxi.get(node).get(0);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().beginTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().iterator().next().endTime);
                    System.out.println(taxi.getCurrentPlans(round_now).getPlans().values().size());
                    System.out.println(taxi.getId());
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

