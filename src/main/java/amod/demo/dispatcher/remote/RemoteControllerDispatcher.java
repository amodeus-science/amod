package amod.demo.dispatcher.remote;

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

import amod.demo.dispatcher.carpooling.CarPooling2DispatcherUtils;
import amod.demo.dispatcher.carpooling.ICRApoolingDispatcher;
import amod.demo.dispatcher.carpooling.ICRApoolingDispatcherUtils;
import amod.demo.dispatcher.carpooling.LinkWait;
import amod.demo.dispatcher.carpooling.PSOControl;
import amod.demo.dispatcher.carpooling.PZOControl;
import amod.demo.dispatcher.carpooling.RebalanceCarSelector;
import amod.demo.dispatcher.carpooling.TravelTimeCalculatorForVirtualNetwork;
import amod.demo.dispatcher.carpooling.XSOControl;
import amod.demo.dispatcher.carpooling.XZOControl;
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
import ch.ethz.idsc.amodeus.lp.LPMinFlow;
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
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.dispatcher.AVDispatcher.AVDispatcherFactory;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.router.AVRouter;

public class RemoteControllerDispatcher extends SharedPartitionedDispatcher {

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
    private RControl rControl;
    private XControl xControl;
    private final int timeStep;
    private final int planningHorizon;
    private final int fixedCarCapacity;
    private final AVRouter router;
    private LinkWait linkWait;
    private final boolean predictedDemand;
    private final boolean allowAssistance;
    private final boolean poolingFlag;
    private List<Link> linkList;
    static private final Logger logger = Logger.getLogger(ICRApoolingDispatcher.class);
    private final int endTime;
    private final int reserveFleet;
    private final boolean discardAVRequetsFlag;
    private int maxDrivingEmptyCars;
    private final boolean checkControlInputsFlag;
    private final boolean skipZeroFlow;
    private final boolean milpFlag;

    protected RemoteControllerDispatcher(Config config, //
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
        this.predictedDemand = false;
        this.allowAssistance = true;
        this.poolingFlag = false;
        this.linkList = ICRApoolingDispatcherUtils.getLinkforStation(network, config, virtualNetwork);
        this.endTime = (int) config.qsim().getEndTime();
        this.reserveFleet = 20;
        this.discardAVRequetsFlag = false;
        this.maxDrivingEmptyCars = 10000;
        this.checkControlInputsFlag = true;
        this.skipZeroFlow = true;
        this.milpFlag = true;
    }

    @Override
    protected void redispatch(double now) {

        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0 && round_now >= dispatchPeriod) {

            // travel times
            Tensor travelTimes = TravelTimeCalculatorVirtualNetwork.computeTravelTimes(virtualNetwork,
                    Quantity.of(now, SI.SECOND), router, linkList);

            Tensor travelTimesStations = RemoteControllerUtils.getTravelTimesInteger(virtualNetwork, timeStep,
                    travelTimes);

            linkWait = new LinkWait(new HashMap<VirtualNode<Link>, List<Link>>());
            HashMap<VirtualNode<Link>, List<Link>> linkMap = linkWait.getLinkWait();

            Pair<Tensor, HashMap<VirtualNode<Link>, List<Link>>> flowsOutpair = RemoteControllerUtils
                    .getPassengersFlowsOut(network, virtualNetwork, planningHorizon, timeStep, config, round_now,
                            linkMap);
            Tensor flowsOut = flowsOutpair.getLeft();
            linkMap = flowsOutpair.getRight();
            linkWait.setLinkWait(linkMap);

            List<AVRequest> requests = getUnassignedAVRequests();
            int[][] pastUnassignedRequests = new int[virtualNetwork.getvNodesCount()][virtualNetwork
                    .getvNodesCount()];
            for (AVRequest req : requests) {
                int fromLink = virtualNetwork.getVirtualNode(req.getFromLink()).getIndex();
                int toLink = virtualNetwork.getVirtualNode(req.getToLink()).getIndex();
                pastUnassignedRequests[fromLink][toLink] = pastUnassignedRequests[fromLink][toLink] + 1;
                linkWait.addLinkWaitElement(virtualNetwork.getVirtualNode(req.getToLink()), req.getFromLink());
            }

            double sumFlow = 0;
            for (int i = 0; i < virtualNetwork.getvNodesCount(); i++) {
                for (int j = 0; j < virtualNetwork.getvNodesCount(); j++) {
                    int flowsOutUnassigned = flowsOut.Get(i, j, 0).number().intValue() + pastUnassignedRequests[i][j];
                    flowsOut.set(DoubleScalar.of(flowsOutUnassigned), i, j, 0);
                    for(int t=0; t<planningHorizon; t++) {
                        sumFlow = flowsOut.Get(i,j,t).number().doubleValue() + sumFlow;
                    }
                }
            }
            
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayRoboTaxi();
            Map<VirtualNode<Link>, List<RoboTaxi>> rebalancingTaxi = getDestinationVirtualNodeRedirectOnlyRoboTaxi();
            Collection<RoboTaxi> oneCustomerRoboTaxi = getRoboTaxisWithNumberOfCustomer(1);

            Tensor rState = RemoteControllerUtils.getRState(round_now, planningHorizon, timeStep, fixedCarCapacity,
                    stayRoboTaxi, rebalancingTaxi, oneCustomerRoboTaxi, virtualNetwork, router);
            
            int sumRstate = 0;
            for(int i=0; i<nVNodes; i++) {
                for(int t=0; t<planningHorizon; t++) {
                    sumRstate = sumRstate + rState.Get(i,t).number().intValue();
                }
            }
            
            if(sumRstate != getRoboTaxis().size()) {
                logger.warn("Not all starter vehicles are captured");
            }
                        
            boolean isFlowsZero;
            
            if(skipZeroFlow==false) {
                isFlowsZero = false;
            } else {
                if(sumFlow==0) {
                    isFlowsZero = true;
                } else {
                    isFlowsZero = false;
                }
                
            }
            
            if(isFlowsZero==false) {
                LPOptimalFlow milpOptimalFlow = new LPOptimalFlow(virtualNetwork, planningHorizon, travelTimesStations, rState,
                        flowsOut, milpFlag);
                milpOptimalFlow.initiateLP();
                milpOptimalFlow.solveLP(false);
                Tensor r_ij = milpOptimalFlow.getr_ij();
                Tensor x_ij = milpOptimalFlow.getx_ij();

                rControl = new RControl(r_ij);
                xControl = new XControl(x_ij);
            } else {
                Tensor r_ij = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
                Tensor x_ij = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
                
                rControl = new RControl(r_ij);
                xControl = new XControl(x_ij);
            }
            
            dispatchTime = round_now;

        }

        // X cars
        if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60))
                || (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests = getVirtualNodeFromAVRequest();
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests = getVirtualNodeToAVRequest();
            Collection<RoboTaxi> emptyDrivingVehicles = getEmptyDrivingRoboTaxis();
            try {
                List<Pair<RoboTaxi, AVRequest>> xZOControlPolicy = xControl.getXCommands(virtualNetwork, stayRoboTaxi,
                        virtualNodeAVFromRequests, virtualNodeAVToRequests, linkList, emptyDrivingVehicles,
                        maxDrivingEmptyCars);
                if (xZOControlPolicy != null) {
                    for (Pair<RoboTaxi, AVRequest> pair : xZOControlPolicy) {
                        RoboTaxi roboTaxi = pair.getLeft();
                        if (!roboTaxi.getMenu().getCourses().isEmpty() && roboTaxi.getMenu().getCourses().size() == 1
                                && roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                            roboTaxi.getMenu().clearWholeMenu();
                        }
                        AVRequest avRequest = pair.getRight();
                        addSharedRoboTaxiPickup(roboTaxi, avRequest);

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

            try {
                Collection<RoboTaxi> emptyDrivingVehicles = getEmptyDrivingRoboTaxis();
                List<Pair<RoboTaxi, Link>> controlPolicy = rControl.getRebalanceCommands(stayRoboTaxi, virtualNetwork,
                        linkList, emptyDrivingVehicles, maxDrivingEmptyCars);
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

                        if (!roboTaxi.getMenu().getCourses().isEmpty() && roboTaxi.getMenu().getCourses().size() == 1
                                && roboTaxi.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                            roboTaxi.getMenu().clearWholeMenu();
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

        // Assign unassigned requests
        if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60) && getRoboTaxisFree().size() > reserveFleet
                && allowAssistance == true)
                || (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)
                        && getRoboTaxisFree().size() > 100 && allowAssistance == true)) {
            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
                for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
                    Tensor controlLawX = xControl.getControlLawX();
                    double x = controlLawX.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();
                    if (x == 0) {
                        List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
                        List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
                        List<AVRequest> fromToRequests = fromRequests.stream().filter(req -> toRequests.contains(req))
                                .collect(Collectors.toList());
                        if (!fromToRequests.isEmpty()) {
                            for (AVRequest avRequest : fromToRequests) {
                                Collection<RoboTaxi> availableCars = new ArrayList<RoboTaxi>();

                                availableCars = getRoboTaxisAvailableSO();

                                if (availableCars.isEmpty()) {
                                    continue;
                                }

                                Collection<RoboTaxi> emptyDrivingVehicles = getEmptyDrivingRoboTaxis();

                                if (emptyDrivingVehicles.size() >= maxDrivingEmptyCars) {
                                    List<RoboTaxi> rebalancingCars = availableCars.stream()
                                            .filter(car -> !car.getMenu().getCourses().isEmpty() && car.getMenu()
                                                    .getStarterCourse().getMealType().equals(SharedMealType.REDIRECT))
                                            .collect(Collectors.toList());
                                    if (rebalancingCars.isEmpty()) {
                                        continue;
                                    }

                                    availableCars = rebalancingCars;
                                }

                                RoboTaxi closestRoboTaxi = StaticHelperRemote.findClostestVehicle(avRequest,
                                        availableCars);
                                if (!closestRoboTaxi.getMenu().getCourses().isEmpty() && closestRoboTaxi.getMenu()
                                        .getCourses().get(0).getMealType() == SharedMealType.REDIRECT) {
                                    closestRoboTaxi.getMenu().removeAVCourse(0);
                                    GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());
                                }

                                addSharedRoboTaxiPickup(closestRoboTaxi, avRequest);
                                GlobalAssert.that(closestRoboTaxi.getMenu().getCourses().size() == 2);
                                GlobalAssert.that(closestRoboTaxi.checkMenuConsistency());

                            }

                        }
                    }

                }
            }

        }

        if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60) && discardAVRequetsFlag == true)
                || (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)
                        && discardAVRequetsFlag == true)) {
            List<AVRequest> avrequests = getUnassignedAVRequests();
            for (AVRequest avreq : avrequests) {
                if (avreq.getSubmissionTime() + 15 * 60 > round_now) {
                    avreq.getPassenger().endActivityAndComputeNextState(now);
                }
            }
        }

        if ((round_now % 10 == 0 && round_now > dispatchPeriod && round_now >= dispatchTime
                && round_now < (dispatchTime + timeStep * 60))
                || (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60))) {

            Collection<RoboTaxi> soRoboTaxis = getRoboTaxisWithNumberOfCustomer(1);
            Collection<RoboTaxi> emptyRoboTaxis = getRoboTaxisWithNumberOfCustomer(0);
            System.out.println("Number of SO Cars: " + soRoboTaxis.size());
            System.out.println("Number of empty Cars: " + emptyRoboTaxis.size());
            Collection<RoboTaxi> emptyDrivingVehicles = getEmptyDrivingRoboTaxis();
            System.out.println("Number of empty driving vehicles: " + emptyDrivingVehicles.size());
        }

        // check if control inputs used
        if (round_now > dispatchPeriod && round_now == (dispatchTime - 1 + timeStep * 60)
                && checkControlInputsFlag == true) {
            Tensor controlLawX = xControl.getControlLawX();
            Tensor controlLawRebalance = rControl.getControlLawRebalance();
            double numberRebalance = 0;
            double numberX = 0;
            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
                for (VirtualNode<Link> toNode : virtualNetwork.getVirtualNodes()) {
                    double rebalancequeue = controlLawRebalance.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();
                    numberRebalance = numberRebalance + rebalancequeue;
                    double xque = controlLawX.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();
                    numberX = numberX + xque;
                    if (xque != 0) {
                        List<AVRequest> fromRequests = getVirtualNodeFromAVRequest().get(fromNode);
                        List<AVRequest> toRequests = getVirtualNodeToAVRequest().get(toNode);
                        List<RoboTaxi> freecar = getVirtualNodeStayWithoutCustomerOrRebalanceRoboTaxi().get(fromNode);
                        List<AVRequest> fromToRequest = fromRequests.stream().filter(req -> toRequests.contains(req))
                                .collect(Collectors.toList());
                        GlobalAssert.that(freecar.isEmpty() || fromToRequest.isEmpty());
                    }
                }
            }

            if (numberRebalance == 0) {
                System.out.println("all rebalance commands used");
            } else {
                logger.warn("NOT ALL rebalance commands used");
            }

            if (numberX == 0) {
                System.out.println("all X commands used");
            } else {
                logger.warn("NOT ALL X commands used");
            }

            List<AVRequest> unassignedRequests = getUnassignedAVRequests();
            Collection<AVRequest> totRequest = getAVRequests();
            System.out.println("Number of open total requests: " + totRequest.size());
            if (!unassignedRequests.isEmpty()) {
                logger.warn("Open Requests");
                System.out.println("Number of open requests: " + unassignedRequests.size());
            }

        }

    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayRoboTaxi() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(RoboTaxiStatus.STAY), RoboTaxi::getDivertableLocation);
    }

    private Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayWithoutCustomerRoboTaxi() {
        List<RoboTaxi> taxiList = getDivertableUnassignedRoboTaxis().stream()
                .filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
                        && car.getMenu().getCourses().isEmpty())
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation())))
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

    private Map<VirtualNode<Link>, List<RoboTaxi>> getDestinationVirtualNodeRedirectOnlyRoboTaxi() {
        List<RoboTaxi> rebalancingTaxi = getRoboTaxisWithNumberOfCustomer(0).stream()
                .filter(car -> car.getMenu().getCourses().size() == 1
                        && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT)
                .collect(Collectors.toList());
        return virtualNetwork.binToVirtualNode(rebalancingTaxi, RoboTaxi::getCurrentDriveDestination);
    }

    private Map<VirtualNode<Link>, List<AVRequest>> getVirtualNodeFromAVRequest() {
        return virtualNetwork.binToVirtualNode(getUnassignedAVRequests(), AVRequest::getFromLink);
    }

    private Map<VirtualNode<Link>, List<AVRequest>> getVirtualNodeToAVRequest() {
        return virtualNetwork.binToVirtualNode(getUnassignedAVRequests(), AVRequest::getToLink);
    }

    private Collection<RoboTaxi> getRoboTaxisFree() {
        return getRoboTaxis().stream()
                .filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
                        && car.getMenu().getCourses().isEmpty())
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation())))
                .collect(Collectors.toList());
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
                        || (!car.getMenu().getCourses().isEmpty()
                                && car.getMenu().getStarterCourse().getMealType() == SharedMealType.WAITFORCUSTOMER
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

    protected final Collection<RoboTaxi> getRoboTaxisAvailableSO() {
        List<RoboTaxi> availableCars = getRoboTaxis().stream() //
                .filter(car -> (car.isInStayTask() && car.getCurrentNumberOfCustomersOnBoard() == 0
                        && car.getMenu().getCourses().isEmpty())
                        || (car.getMenu().getCourses().size() == 1
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT
                                && virtualNetwork.getVirtualNode(car.getCurrentDriveDestination()) == virtualNetwork
                                        .getVirtualNode(car.getDivertableLocation())))
                .collect(Collectors.toList());
        return availableCars;
    }

    protected final Collection<RoboTaxi> getEmptyDrivingRoboTaxis() {
        List<RoboTaxi> availableCars = getRoboTaxis().stream() //
                .filter(car -> (car.getCurrentNumberOfCustomersOnBoard() == 0 && car.getMenu().getCourses().size() == 1
                        && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.REDIRECT)
                        || (car.getMenu().getCourses().size() == 2
                                && car.getMenu().getCourses().get(0).getMealType() == SharedMealType.PICKUP))
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

        @Inject
        private MatsimAmodeusDatabase db;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(
                    EuclideanDistanceFunction.INSTANCE);

            return new RemoteControllerDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager,
                    network, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelData, db);
        }
    }

}
