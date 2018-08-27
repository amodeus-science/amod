package amod.demo.dispatcher.SMPC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.schedule.AVTask;
import ch.ethz.matsim.av.schedule.AVTask.AVTaskType;

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

    public static double[][] getAvailableCars(double time, int planningHorizon, int timeStep,
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> rebalanceRoboTaxi,
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi, VirtualNetwork<Link> virtualNetwork, Map<VirtualLink<Link>, Double> travelTimes) {
        int NumberNodes = virtualNetwork.getvNodesCount();

        double[][] TotalAvailableCars = new double[planningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> StayCarsAtNode = stayRoboTaxi.get(node);

            List<RoboTaxi> rebalanceCarsAtNode = rebalanceRoboTaxi.get(node);
            List<RoboTaxi> soCarsAtNode = soRoboTaxi.get(node);

            
            if (StayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = StayCarsAtNode.size();
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

                if (t == 0) {
                    TotalAvailableCars[t][node.getIndex()] = numberStay + numberReb + numberSO;
                } else {
                    TotalAvailableCars[t][node.getIndex()] = numberReb + numberSO;
                }

            }

        }

        return TotalAvailableCars;

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
