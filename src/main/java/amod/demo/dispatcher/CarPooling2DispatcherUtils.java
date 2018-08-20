package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
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

    @SuppressWarnings("unchecked")
    public static double[][] getRState(double Time, int PlanningHorizon, int timeStep, Collection<AVRequest> avRequests, int fixedCarCapacity,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi, List<RoboTaxi> taxiWithCustomer,
            List<RoboTaxi> taxiRebalancing, VirtualNetwork<Link> virtualNetwork,
            Map<VirtualLink<Link>, Double> TravelTimes) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        List<AVRequest> oneCustomer = new ArrayList<>();
        List<AVRequest> twoCustomer = new ArrayList<>();
        List<RoboTaxi> taxiWithOneCustomer = new ArrayList<>();
        List<RoboTaxi> taxiWithTwoCustomer = new ArrayList<>();
        
        if(!taxiWithCustomer.isEmpty()) {
            taxiWithOneCustomer = (List<RoboTaxi>) taxiWithCustomer.stream()
                    .filter(car -> car.getCurrentNumberOfCustomersOnBoard() == 1);
            taxiWithTwoCustomer = (List<RoboTaxi>) taxiWithCustomer.stream()
                    .filter(car -> car.getCurrentNumberOfCustomersOnBoard() == fixedCarCapacity);
        }
        
        if(!taxiWithOneCustomer.isEmpty()) {
            for (RoboTaxi taxiOne : taxiWithOneCustomer) {
                AVRequest request = (AVRequest) avRequests.stream()
                        .filter(re -> taxiOne.getMenu().getCourses().get(0).getRequestId() == re.getId().toString());
                oneCustomer.add(request);
            }
        }
        
       if(!taxiWithTwoCustomer.isEmpty()) {
           for (RoboTaxi taxiTwo : taxiWithTwoCustomer) {
               AVRequest request2 = (AVRequest) avRequests.stream()
                       .filter(re -> taxiTwo.getMenu().getCourses().get(1).getRequestId() == re.getId().toString());
               twoCustomer.add(request2);
           }
       }


        double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;
        int numberDO;

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> StayCarsAtNode = StayRoboTaxi.get(node);

            List<RoboTaxi> rebalancingNode = new ArrayList<>();
            
            if(!taxiRebalancing.isEmpty()) {
                rebalancingNode = (List<RoboTaxi>) taxiRebalancing.stream()
                        .filter(car -> node.getLinks().contains(car.getCurrentDriveDestination()));
            }
            

            if (StayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = StayCarsAtNode.size();
            }

            for (int t = 0; t < PlanningHorizon; t++) {
                if (rebalancingNode.isEmpty() == true) {
                    numberReb = 0;
                } else {
                    numberReb = getNumberCarsAbailableAtTime(Time, t, timeStep, rebalancingNode, virtualNetwork,
                            TravelTimes);
                }

                if (oneCustomer.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getNumberDropoffs(Time, t, timeStep, node, oneCustomer);
                }

                if (twoCustomer.isEmpty() == true) {
                    numberDO = 0;
                } else {
                    numberDO = getNumberDropoffs(Time, t, timeStep, node, twoCustomer);
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

    @SuppressWarnings("unchecked")
    public static List<double[][]> getXState(double Time, int PlanningHorizon, int timeStep, Collection<AVRequest> avRequests,
            int fixedCarCapacity, Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi, List<RoboTaxi> taxiWithCustomer, VirtualNetwork<Link> virtualNetwork) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        List<double[][]> xState = new ArrayList<>(NumberNodes);

        List<AVRequest> firstCustomer = new ArrayList<>();
        List<AVRequest> secondCustomer = new ArrayList<>();
        List<Pair<AVRequest, AVRequest>> customerRequests = new ArrayList<>();
        List<RoboTaxi> taxiWithTwoCustomer = new ArrayList<>();

        if(!taxiWithCustomer.isEmpty()) {
            taxiWithTwoCustomer = (List<RoboTaxi>) taxiWithCustomer.stream()
                    .filter(car -> car.getCurrentNumberOfCustomersOnBoard() == fixedCarCapacity);
        }
        
        if(!taxiWithTwoCustomer.isEmpty()) {
            for (RoboTaxi taxiTwo : taxiWithTwoCustomer) {
                AVRequest request1 = (AVRequest) avRequests.stream()
                        .filter(re -> taxiTwo.getMenu().getStarterCourse().getRequestId() == re.getId().toString());
                AVRequest request2 = (AVRequest) avRequests.stream()
                        .filter(re -> taxiTwo.getMenu().getCourses().get(1).getRequestId() == re.getId().toString());
                firstCustomer.add(request1);
                secondCustomer.add(request2);
                Pair<AVRequest, AVRequest> requestPair = Pair.of(request1, request2);
                customerRequests.add(requestPair);

            }
        }

        

        for (VirtualNode<Link> destNodeSecond : virtualNetwork.getVirtualNodes()) {
            double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
            int numberSO;
            int numberDO;

            for (VirtualNode<Link> destNodeFirst : virtualNetwork.getVirtualNodes()) {
                List<RoboTaxi> soCarsAtNode = SORoboTaxi.get(destNodeFirst);

                if (soCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = soCarsAtNode.size();
                }

                for (int t = 0; t < PlanningHorizon; t++) {

                    if (customerRequests.isEmpty() == true) {
                        numberDO = 0;
                    } else {
                        numberDO = getNumberDropoffsXState(Time, t, timeStep, destNodeFirst, destNodeSecond,
                                customerRequests);
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
        double[][] FlowsOutMatrix = new double[numberVirtualNodes][numberVirtualNodes];

        for (int i = 0; i < PlanningHorizon; i++) {
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

    private static int getNumberCarsAbailableAtTime(double Time, int t, int timeStep, List<RoboTaxi> carsAtNode,
            VirtualNetwork<Link> virtualNetwork, Map<VirtualLink<Link>, Double> TravelTimes) {
        int numberCars = 0;
        for (RoboTaxi car : carsAtNode) {
            Link fromLink = car.getLastKnownLocation();
            Link toLink = car.getCurrentDriveDestination();
            VirtualNode<Link> fromNode = virtualNetwork.getVirtualNode(fromLink);
            VirtualNode<Link> toNode = virtualNetwork.getVirtualNode(toLink);
            for (VirtualLink<Link> link : virtualNetwork.getVirtualLinks()) {
                if (link.getFrom() == fromNode && link.getTo() == toNode) {
                    Double travelTime = TravelTimes.get(link);
                    if (travelTime + Time > Time + t * timeStep * 60
                            && travelTime + Time <= Time + (t + 1) * timeStep * 60) {
                        numberCars = numberCars + 1;
                    }
                    break;
                }
            }
        }
        return numberCars;
    }

    private static int getNumberDropoffs(double Time, int t, int timeStep, VirtualNode<Link> node,
            List<AVRequest> requestList) {
        int numberDropoffs = 0;

        for (AVRequest request : requestList) {
            if (request.getSubmissionTime() > Time + t * timeStep * 60
                    && request.getSubmissionTime() <= Time + (t + 1) * timeStep * 60
                    && node.getLinks().contains(request.getToLink())) {
                numberDropoffs = numberDropoffs + 1;
            }
        }

        return numberDropoffs;
    }

    private static int getNumberDropoffsXState(double Time, int t, int timeStep, VirtualNode<Link> destFirstNode,
            VirtualNode<Link> destSecondNode, List<Pair<AVRequest, AVRequest>> requestsPairs) {
        int numberDropoffs = 0;

        for (Pair<AVRequest, AVRequest> pair : requestsPairs) {
            if (pair.getLeft().getSubmissionTime() > Time + t * timeStep * 60
                    && pair.getLeft().getSubmissionTime() <= Time + (t + 1) * timeStep * 60
                    && destFirstNode.getLinks().contains(pair.getLeft().getToLink())
                    && destSecondNode.getLinks().contains(pair.getRight().getToLink())) {
                numberDropoffs = numberDropoffs + 1;
            }
        }

        return numberDropoffs;
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
}
