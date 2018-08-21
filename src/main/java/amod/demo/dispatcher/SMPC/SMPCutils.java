package amod.demo.dispatcher.SMPC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.matsim.av.passenger.AVRequest;

public enum SMPCutils {
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

    public static double[][] getAvailableCars(double Time, int PlanningHorizon, int timeStep,
            Collection<AVRequest> avRequests, Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi,
            List<RoboTaxi> taxiWithCustomer, List<RoboTaxi> taxiRebalancing, VirtualNetwork<Link> virtualNetwork,
            Map<VirtualLink<Link>, Double> TravelTimes) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        List<AVRequest> oneCustomer = new ArrayList<>();
        List<RoboTaxi> taxiWithOneCustomer = new ArrayList<>();

        if (!taxiWithCustomer.isEmpty()) {
            taxiWithOneCustomer = (List<RoboTaxi>) taxiWithCustomer.stream()
                    .filter(car -> car.getCurrentNumberOfCustomersOnBoard() == 1);

        }

        if (!taxiWithOneCustomer.isEmpty()) {
            for (RoboTaxi taxiOne : taxiWithOneCustomer) {
                AVRequest request = (AVRequest) avRequests.stream()
                        .filter(re -> taxiOne.getMenu().getCourses().get(0).getRequestId() == re.getId().toString());
                oneCustomer.add(request);
            }
        }

        double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> StayCarsAtNode = StayRoboTaxi.get(node);

            List<RoboTaxi> rebalancingNode = new ArrayList<>();

            if (!taxiRebalancing.isEmpty()) {
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

                if (t == 0) {
                    TotalAvailableCars[t][node.getIndex()] = numberStay + numberReb + numberSO;
                } else {
                    TotalAvailableCars[t][node.getIndex()] = numberReb + numberSO;
                }

            }

        }

        return TotalAvailableCars;

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

    public static Container getContainerInit() {

        return new Container("InputSMPC");

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
