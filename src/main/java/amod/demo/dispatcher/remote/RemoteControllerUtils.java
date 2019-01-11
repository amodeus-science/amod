package amod.demo.dispatcher.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import amod.demo.dispatcher.carpooling.ExpectedCarPoolingArrival;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.prep.PopulationTools;
import ch.ethz.idsc.amodeus.prep.Request;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.router.AVRouter;

public enum RemoteControllerUtils {
    ;

    public static Tensor getTravelTimesInteger(VirtualNetwork<Link> virtualNetwork, int timeStep, Tensor travelTimes) {

        int nodeNumber = virtualNetwork.getvNodesCount();
        Tensor travelTimesInteger = Array.zeros(nodeNumber, nodeNumber);
        Collection<VirtualLink<Link>> vLinks = virtualNetwork.getVirtualLinks();

        for (VirtualLink<Link> link : vLinks) {
            int fromNode = link.getFrom().getIndex();
            int toNode = link.getTo().getIndex();
            double timeI = Math.round(travelTimes.Get(fromNode, toNode).number().doubleValue() / (timeStep * 60));
            travelTimesInteger.set(DoubleScalar.of(timeI), fromNode, toNode);
            if (travelTimesInteger.Get(fromNode, toNode).number().doubleValue() == 0) {
                travelTimesInteger.set(DoubleScalar.of(1), fromNode, toNode);
            }

        }

        for (int i = 0; i < nodeNumber; ++i) {
            travelTimesInteger.set(DoubleScalar.of(1), i, i);
        }

        return travelTimesInteger;

    }

    public static Tensor getRState(double time, int planningHorizon, int timeStep, int fixedCarCapacity,
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
            Map<VirtualNode<Link>, List<RoboTaxi>> rebalanceRoboTaxi, Collection<RoboTaxi> soRoboTaxi,
            VirtualNetwork<Link> virtualNetwork, AVRouter router) {
        Tensor totalAvailableCars = Array.zeros(virtualNetwork.getvNodesCount(), planningHorizon);
        int numberStay;
        int numberReb;
        int numberSO;

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> stayCarsAtNode = stayRoboTaxi.get(node);
            List<RoboTaxi> rebalanceCarsAtNode = rebalanceRoboTaxi.get(node);
            List<RoboTaxi> soCarsAtNode = soRoboTaxi.stream()
                    .filter(car -> car.getMenu().getCourses().size() == 1
                            && node.getLinks().contains(car.getMenu().getCourses().get(0).getLink()))
                    .collect(Collectors.toList());

            if (stayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = stayCarsAtNode.size();
            }

            for (int t = 0; t < planningHorizon; t++) {
                if (rebalanceCarsAtNode.isEmpty() == true) {
                    numberReb = 0;
                } else {
                    numberReb = getNumberOfArrivingCars(time, t, timeStep, rebalanceCarsAtNode, router);
                }

                if (soCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getNumberOfArrivingCars(time, t, timeStep, soCarsAtNode, router);
                }

                if (t == 0) {
                    int totalAvaiable = numberStay + numberReb + numberSO;
                    totalAvailableCars.set(DoubleScalar.of(totalAvaiable), node.getIndex(), t);

                } else {
                    int totalAvaiable = numberReb + numberSO;
                    totalAvailableCars.set(DoubleScalar.of(totalAvaiable), node.getIndex(), t);
                }

            }

        }

        return totalAvailableCars;

    }

    
    public static Pair<List<double[][]>, HashMap<VirtualNode<Link>, List<Link>>> getFlowsOut(Network network,
            VirtualNetwork<Link> virtualNetwork, int PlanningHorizon, int timeStep, Config config, double round_now,
            HashMap<VirtualNode<Link>, List<Link>> linkMap) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
        List<double[][]> dataList = new ArrayList<>();

        for (VirtualNode<Link> vNod : virtualNetwork.getVirtualNodes()) {
            List<Link> linkList = new ArrayList<>();
            linkMap.put(vNod, linkList);
        }

        int helper = 0;
        int FromnodeIndex = 0;
        int TonodeIndex = 0;
        int numberVirtualNodes = virtualNetwork.getVirtualNodes().size();
        Link fromLink = null;
        for (int i = 0; i < PlanningHorizon; i++) {
            double[][] FlowsOutMatrix = new double[numberVirtualNodes][numberVirtualNodes];
            for (Person person : population.getPersons().values()) {
                for (Plan plan : person.getPlans()) {
                    for (PlanElement planElement : plan.getPlanElements()) {
                        if (planElement instanceof Activity) {
                            Activity activity = (Activity) planElement;

                            if (activity.getEndTime() >= (round_now + i * timeStep * 60)
                                    && activity.getEndTime() <= (round_now + (i + 1) * timeStep * 60)) {
                                if (!stageActivityTypes.isStageActivity(activity.getType())) {
                                    fromLink = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                    if (fromLink != null) {
                                        VirtualNode<Link> fromVirtualNode = virtualNetwork.getVirtualNode(fromLink);
                                        FromnodeIndex = fromVirtualNode.getIndex();
                                        helper = 1;
                                    }
                                }
                            }

                            if (activity.getStartTime() != Double.NEGATIVE_INFINITY && helper == 1) {
                                if (!stageActivityTypes.isStageActivity(activity.getType())) {
                                    Link link = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                    if (link != null) {
                                        VirtualNode<Link> ToVirtualNode = virtualNetwork.getVirtualNode(link);
                                        TonodeIndex = ToVirtualNode.getIndex();
                                        FlowsOutMatrix[FromnodeIndex][TonodeIndex] = FlowsOutMatrix[FromnodeIndex][TonodeIndex]
                                                + 1;
                                        if (i == 0) {
                                            linkMap.get(ToVirtualNode).add(fromLink);
                                        }
                                        helper = 0;
                                    }
                                }
                            }

                        }

                    }
                }
            }
            dataList.add(i, FlowsOutMatrix);
        }
        Pair<List<double[][]>, HashMap<VirtualNode<Link>, List<Link>>> pairReturn = Pair.of(dataList, linkMap);
        return pairReturn;

    }

    public static Pair<Tensor, HashMap<VirtualNode<Link>, List<Link>>> getPassengersFlowsOut(Network network,
            VirtualNetwork<Link> virtualNetwork, int planningHorizon, int timeStep, Config config, double round_now,
            HashMap<VirtualNode<Link>, List<Link>> linkMap) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        Tensor flowsOut = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount(),
                planningHorizon);
        for (VirtualNode<Link> vNod : virtualNetwork.getVirtualNodes()) {
            List<Link> linkList = new ArrayList<>();
            linkMap.put(vNod, linkList);
        }
        int endTime = (int) config.qsim().getEndTime();
        Set<Request> avRequests = PopulationTools.getAVRequests(population, network, endTime);
        double flowsCounter = 0;
        for (int t = 0; t < planningHorizon; t++) {
            for (Request avRequest : avRequests) {
                double startTime = avRequest.startTime();
                if (startTime >= (round_now + t * timeStep * 60)
                        && startTime <= (round_now + (t + 1) * timeStep * 60)) {
                    Link fromLink = avRequest.startLink();
                    Link toLink = avRequest.endLink();
                    if (fromLink != null && toLink != null) {
                        int fromVirtualNode = virtualNetwork.getVirtualNode(fromLink).getIndex();
                        int toVirtualNode = virtualNetwork.getVirtualNode(toLink).getIndex();
                        flowsCounter = flowsOut.Get(fromVirtualNode, toVirtualNode, t).number().intValue() + 1;
                        flowsOut.set(DoubleScalar.of(flowsCounter), fromVirtualNode, toVirtualNode, t);
                        linkMap.get(virtualNetwork.getVirtualNode(toLink)).add(fromLink);
                    }

                }
            }
        }

        Pair<Tensor, HashMap<VirtualNode<Link>, List<Link>>> pairReturn = Pair.of(flowsOut, linkMap);
        return pairReturn;

    }

    private static int getNumberOfArrivingCars(double Time, int t, int timeStep, List<RoboTaxi> roboTaxiList,
            AVRouter router) {

        int numberCars = 0;
        for (RoboTaxi roboTaxi : roboTaxiList) {
            Map<String, Scalar> arrivals = ExpectedCarPoolingArrival.of(roboTaxi, Time, router);
            if (roboTaxi.getCurrentNumberOfCustomersOnBoard() == 0) {
                Scalar arrivalTimeReb = arrivals.get(roboTaxi.getMenu().getStarterCourse().getRequestId());
                if (arrivalTimeReb.number().doubleValue() > Time + t * timeStep * 60
                        && arrivalTimeReb.number().doubleValue() <= Time + (t + 1) * timeStep * 60) {
                    numberCars = numberCars + 1;
                }
            }

            if (roboTaxi.getCurrentNumberOfCustomersOnBoard() == 1) {
                Scalar arrivalTimeSO = null;
                if (roboTaxi.getMenu().getCourses().size() > 1) {
                    arrivalTimeSO = arrivals.get(roboTaxi.getMenu().getCourses().get(0).getRequestId());
                } else {
                    arrivalTimeSO = arrivals.get(roboTaxi.getMenu().getStarterCourse().getRequestId());
                }

                if (arrivalTimeSO.number().doubleValue() > Time + t * timeStep * 60
                        && arrivalTimeSO.number().doubleValue() <= Time + (t + 1) * timeStep * 60) {
                    numberCars = numberCars + 1;
                }
            }

            if (roboTaxi.getCurrentNumberOfCustomersOnBoard() == 2) {
                Scalar arrivalTimeDO = arrivals.get(roboTaxi.getMenu().getCourses().get(1).getRequestId());
                if (arrivalTimeDO.number().doubleValue() > Time + t * timeStep * 60
                        && arrivalTimeDO.number().doubleValue() <= Time + (t + 1) * timeStep * 60) {
                    numberCars = numberCars + 1;
                }
            }

        }

        return numberCars;
    }


    public static List<List<AVRequest>> getFromToAVRequests(VirtualNetwork<Link> virtualNetwork,
            List<AVRequest> fromRequest, Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) {
        List<List<AVRequest>> fromToAVRequests = new ArrayList<>(virtualNetwork.getvNodesCount());

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {

            List<AVRequest> fromToRequests = fromRequest.stream()
                    .filter(rt -> VirtualNodeAVToRequests.get(node).contains(rt)).collect(Collectors.toList());
            fromToAVRequests.add(node.getIndex(), fromToRequests);

        }

        return fromToAVRequests;
    }

    public static List<Link> getLinkforStation(Network network, Config config, VirtualNetwork<Link> virtualNetwork) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

        HashMap<VirtualNode<Link>, List<Coord>> coordMap = new HashMap<VirtualNode<Link>, List<Coord>>();
        for (VirtualNode<Link> vNod : virtualNetwork.getVirtualNodes()) {
            List<Coord> linkList = new ArrayList<>();
            coordMap.put(vNod, linkList);
        }
        int helper = 0;

        Link fromLink = null;
        Coord fromCoord = null;

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;

                        if (activity.getEndTime() != Double.NEGATIVE_INFINITY) {
                            if (!stageActivityTypes.isStageActivity(activity.getType())) {
                                fromLink = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                if (fromLink != null) {
                                    fromCoord = fromLink.getCoord();
                                    helper = 1;
                                }
                            }
                        }

                        if (activity.getStartTime() != Double.NEGATIVE_INFINITY && helper == 1) {
                            if (!stageActivityTypes.isStageActivity(activity.getType())) {
                                Link link = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                if (link != null) {
                                    coordMap.get(virtualNetwork.getVirtualNode(fromLink)).add(fromCoord);
                                    helper = 0;
                                }
                            }
                        }

                    }

                }
            }
        }

        List<Link> linkList = new ArrayList<Link>(virtualNetwork.getvNodesCount());

        for (int i = 0; i < virtualNetwork.getvNodesCount(); i++) {
            linkList.add(i, null);
        }

        for (int index = 0; index < virtualNetwork.getvNodesCount(); index++) {
            Link destination = null;
            VirtualNode<Link> virtnode = virtualNetwork.getVirtualNode(index);
            List<Coord> coordinates = coordMap.get(virtnode);

            double xsum = 0;
            double ysum = 0;

            if (coordinates.size() == 0) {
                destination = virtualNetwork.getVirtualNode(index).getLinks().iterator().next();
            } else {
                for (int i = 0; i < coordinates.size(); i++) {
                    xsum = coordinates.get(i).getX() + xsum;
                    ysum = coordinates.get(i).getY() + ysum;
                }

                double xCoord = xsum / coordinates.size();
                double yCoord = ysum / coordinates.size();

                Coord coord = new Coord(xCoord, yCoord);

                destination = NetworkUtils.getNearestLink(network, coord);
            }
            linkList.set(index, destination);

        }

        return linkList;

    }

}
