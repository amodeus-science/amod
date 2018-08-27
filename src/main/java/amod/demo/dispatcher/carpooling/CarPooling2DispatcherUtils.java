package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.config.Config;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.schedule.AVTask;
import ch.ethz.matsim.av.schedule.AVTask.AVTaskType;

public enum CarPooling2DispatcherUtils {
    ;

    public static double[][] getVirtualNetworkForMatlab(VirtualNetwork<Link> virtualNetwork) {
        int NodeNumber = virtualNetwork.getVirtualNodes().size();
        double[][] VirtualNetworkMatrix = new double[NodeNumber][NodeNumber];

        for (VirtualLink<Link> link : virtualNetwork.getVirtualLinks()) {
            int FromNode = link.getFrom().getIndex();
            int ToNode = link.getTo().getIndex();
            VirtualNetworkMatrix[FromNode][ToNode] = ToNode + 1;
        }

        for (int i = 0; i < VirtualNetworkMatrix.length; ++i) {
            VirtualNetworkMatrix[i][i] = i + 1;
        }

        return VirtualNetworkMatrix;

    }

    public static double[][] getTravelTimesVirtualNetworkForMatlab(VirtualNetwork<Link> virtualNetwork, int timeStep,
            Map<VirtualLink<Link>, Double> TravelTimes) {

        int NodeNumber = virtualNetwork.getVirtualNodes().size();
        double[][] travelTimesMat = new double[NodeNumber][NodeNumber];

        for (VirtualLink<Link> link : TravelTimes.keySet()) {
            int FromNode = link.getFrom().getIndex();
            int ToNode = link.getTo().getIndex();
            travelTimesMat[FromNode][ToNode] = Math.round(TravelTimes.get(link) / (timeStep * 60));
            if(travelTimesMat[FromNode][ToNode] == 0) {
                travelTimesMat[FromNode][ToNode] = 1;
            }

        }

        for (int i = 0; i < travelTimesMat.length; ++i) {
            travelTimesMat[i][i] = 1;
        }

        return travelTimesMat;

    }

    public static List<Pair<Integer, Link>> getLinkofVirtualNode(VirtualNetwork<Link> virtualNetwork) {

        List<Pair<Integer, Link>> linkList = new ArrayList<>();

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            Optional<Link> linkAny = node.getLinks().stream().findAny();
            Link link = linkAny.get();

            Pair<Integer, Link> pair = Pair.of(node.getIndex(), link);
            linkList.add(node.getIndex(), pair);

        }

        return linkList;
    }

    public static double[][] getRState(double time, int planningHorizon, int timeStep, int fixedCarCapacity,
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> rebalanceRoboTaxi,
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi, Collection<RoboTaxi> doRoboTaxi, VirtualNetwork<Link> virtualNetwork, Map<VirtualLink<Link>, Double> travelTimes) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        double[][] TotalAvailableCars = new double[planningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;
        int numberDO;

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> stayCarsAtNode = stayRoboTaxi.get(node);
            List<RoboTaxi> rebalanceCarsAtNode = rebalanceRoboTaxi.get(node);
            List<RoboTaxi> soCarsAtNode = soRoboTaxi.get(node);
            
            List<RoboTaxi> doFiltered = doRoboTaxi.stream().filter(car -> car.getMenu().getCourses().size() == 2 && node.getLinks().contains(car.getMenu().getCourses().get(1).getLink())).collect(Collectors.toList());
            
            if (stayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = stayCarsAtNode.size();
            }

            for (int t = 0; t < planningHorizon; t++) {
                if (rebalanceCarsAtNode.isEmpty() == true) {
                    numberReb = 0;
                } else {
                    numberReb = getFinishingTaskAtTimeCars(time, t, timeStep, rebalanceCarsAtNode);
                }

                if (soCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getFinishingTaskAtTimeCars(time, t, timeStep, soCarsAtNode);
                }

                if (doFiltered.isEmpty() == true) {
                    numberDO = 0;
                } else {
                    numberDO = getNumberOfTwoCustomerCarVacant(time, t, timeStep, doFiltered,
                            virtualNetwork, node, travelTimes);
                }

                if (t == 0) {
                    TotalAvailableCars[t][node.getIndex()] = numberStay + numberReb + numberSO + numberDO;
                } else {
                    TotalAvailableCars[t][node.getIndex()] = numberReb + numberSO + numberDO;
                }

            }

        }

        return TotalAvailableCars;

    }

    public static List<double[][]> getXState(double time, int planningHorizon, int timeStep, int fixedCarCapacity,
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> doRoboTaxi, VirtualNetwork<Link> virtualNetwork) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        List<double[][]> xState = new ArrayList<>(NumberNodes);

        for (VirtualNode<Link> destNodeSecond : virtualNetwork.getVirtualNodes()) {
            double[][] TotalAvailableCars = new double[planningHorizon][NumberNodes];
            int numberSO;
            int numberDO;

            for (VirtualNode<Link> destNodeFirst : virtualNetwork.getVirtualNodes()) {
                List<RoboTaxi> soCarsAtNodeNonfiltered = soRoboTaxi.get(destNodeFirst);
                List<RoboTaxi> soCarsAtNode = soCarsAtNodeNonfiltered.stream().filter(cars -> destNodeSecond.getLinks().contains(cars.getCurrentDriveDestination())).collect(Collectors.toList());
                List<RoboTaxi> doCarsAtNode = doRoboTaxi.get(destNodeFirst);
                List<RoboTaxi> doFiltered = doCarsAtNode.stream().filter(car -> destNodeSecond.getLinks().contains(car.getMenu().getCourses().get(1).getLink())).collect(Collectors.toList());

                if (soCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = soCarsAtNode.size();
                }

                for (int t = 0; t < planningHorizon; t++) {

                    if (doFiltered.isEmpty() == true) {
                        numberDO = 0;
                    } else {
                        numberDO = getFinishingTaskAtTimeCars(time, t, timeStep, doFiltered);
                    }

                    if (t == 0) {
                        TotalAvailableCars[t][destNodeFirst.getIndex()] = numberSO + numberDO;
                    } else {
                        TotalAvailableCars[t][destNodeFirst.getIndex()] = numberDO;
                    }

                }

            }
            xState.add(destNodeSecond.getIndex(), TotalAvailableCars);
        }

        return xState;

    }

    public static List<double[][]> getFlowsOut(Network network, VirtualNetwork<Link> virtualNetwork,
            int PlanningHorizon, int timeStep, Config config, double round_now) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
        List<double[][]> dataList = new ArrayList<>();

        int helper = 0;
        int FromnodeIndex = 0;
        int TonodeIndex = 0;
        int numberVirtualNodes = virtualNetwork.getVirtualNodes().size();
        

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
                                    Link link = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                    if (link != null) {
                                        VirtualNode<Link> FromVirtualNode = virtualNetwork.getVirtualNode(link);
                                        FromnodeIndex = FromVirtualNode.getIndex();
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
        return dataList;

    }

    private static int getNumberOfTwoCustomerCarVacant(double Time, int t, int timeStep, List<RoboTaxi> carsToNode,
            VirtualNetwork<Link> virtualNetwork, VirtualNode<Link> toNode, Map<VirtualLink<Link>, Double> TravelTimes) {
        int numberCars = 0;
        for (RoboTaxi car : carsToNode) {
            Link fromLink = car.getLastKnownLocation();
            VirtualNode<Link> fromNode = virtualNetwork.getVirtualNode(fromLink);
            Schedule scheduleRoboTaxi = car.getSchedule();
            AVTask avtask = (AVTask) scheduleRoboTaxi.getCurrentTask();
            double firstEndTime = avtask.getEndTime();
            for (VirtualLink<Link> link : virtualNetwork.getVirtualLinks()) {
                if (link.getFrom() == fromNode && link.getTo() == toNode) {
                    Double travelTime = TravelTimes.get(link);
                    if (travelTime + firstEndTime  > Time + t * timeStep * 60
                            && travelTime + firstEndTime <= Time + (t + 1) * timeStep * 60) {
                        numberCars = numberCars + 1;
                    }
                    break;
                }
            }
        }
        return numberCars;
    }

    private static int getFinishingTaskAtTimeCars(double Time, int t, int timeStep, List<RoboTaxi> roboTaxiList) {
        
        int numberCars = 0;
        for (RoboTaxi roboTaxi : roboTaxiList) {
            Schedule scheduleRoboTaxi = roboTaxi.getSchedule();
            AVTask avtask = (AVTask) scheduleRoboTaxi.getCurrentTask();
            if (avtask.getEndTime() > Time + t * timeStep * 60
                    && avtask.getEndTime() <= Time + (t + 1) * timeStep * 60 && avtask.getAVTaskType() == AVTaskType.DRIVE) {
                numberCars = numberCars + 1;
            }
        }

        return numberCars;
    }

    public static void printArray(Container container, String field) {
        if (container.contains(field)) {
            DoubleArray doubleArray = container.get(field);
            System.out.println(doubleArray);
            for (int index = 0; index < doubleArray.value.length; ++index)
                System.out.print("[" + index + "]=" + doubleArray.value[index] + ", ");
            System.out.println();
        } else {
            System.out.println(" !!! field '" + field + "' not present !!! ");
        }
    }

    public static double[] getArray(Container container, String field) {
        if (container.contains(field)) {
            DoubleArray doubleArray = container.get(field);
            double[] array = doubleArray.value;
            return array;
        } else {
            throw new EmptyStackException();
        }

    }
    
    public static List<List<AVRequest>> getFromToAVRequests(VirtualNetwork<Link> virtualNetwork, List<AVRequest> fromRequest,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) {
        List<List<AVRequest>> fromToAVRequests = new ArrayList<>(virtualNetwork.getvNodesCount());

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
             
            List<AVRequest> fromToRequests = fromRequest.stream()
                    .filter(rt -> VirtualNodeAVToRequests.get(node).contains(rt)).collect(Collectors.toList());       
            fromToAVRequests.add(node.getIndex(), fromToRequests);            
               
        }

        return fromToAVRequests;
    }
}
