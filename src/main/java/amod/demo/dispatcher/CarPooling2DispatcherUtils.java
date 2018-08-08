package amod.demo.dispatcher;

import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlanEntry;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;

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

    public static double[][] getTravelTimesVirtualNetworkForMatlab(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualLink<Link>, Double> TravelTimes) {

        int NodeNumber = virtualNetwork.getVirtualNodes().size();
        double[][] travelTimesMat = new double[NodeNumber][NodeNumber];

        for (VirtualLink<Link> link : TravelTimes.keySet()) {
            int FromNode = link.getFrom().getIndex();
            int ToNode = link.getTo().getIndex();
            travelTimesMat[FromNode][ToNode] = Math.round(TravelTimes.get(link) / (5 * 60));

        }

        for (int i = 0; i < travelTimesMat.length; ++i) {
            travelTimesMat[i][i] = 1;
        }

        return travelTimesMat;

    }

    public static double[][] getRState(double Time, int PlanningHorizon,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> RebalanceRoboTaxi,
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi) {

        int NumberNodes = StayRoboTaxi.keySet().size();

        if (RebalanceRoboTaxi.keySet().size() != NumberNodes || SORoboTaxi.keySet().size() != NumberNodes) {
            throw new RuntimeException();
        }

        double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;
        int numberDO;

        for (VirtualNode<Link> node : StayRoboTaxi.keySet()) {
            List<RoboTaxi> StayCarsAtNode = StayRoboTaxi.get(node);
            List<RoboTaxi> RebCarsAtNode = RebalanceRoboTaxi.get(node);
            List<RoboTaxi> SOCarsAtNode = SORoboTaxi.get(node);
            List<RoboTaxi> DOCarsAtNode = DORoboTaxi.get(node);

            if (StayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = StayCarsAtNode.size();
            }

            for (int t = 0; t < PlanningHorizon; t++) {
                if (RebCarsAtNode.isEmpty() == true) {
                    numberReb = 0;
                } else {
                    numberReb = getNumberCarsAbailableAtTime(Time, t, RebCarsAtNode);
                }

                if (SOCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getNumberCarsAbailableAtTime(Time, t, SOCarsAtNode);
                }
                
                if (DOCarsAtNode.isEmpty() == true) {
                    numberDO = 0;
                } else {
                    numberDO = getNumberCarsAbailableAtTime(Time, t, DOCarsAtNode);
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
    
    public static double[][] getXState(double Time, int PlanningHorizon,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> RebalanceRoboTaxi,
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi, Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi) {

        int NumberNodes = StayRoboTaxi.keySet().size();

        if (RebalanceRoboTaxi.keySet().size() != NumberNodes || SORoboTaxi.keySet().size() != NumberNodes) {
            throw new RuntimeException();
        }

        double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
        int numberStay;
        int numberReb;
        int numberSO;
        int numberDO;

        for (VirtualNode<Link> node : StayRoboTaxi.keySet()) {
            List<RoboTaxi> StayCarsAtNode = StayRoboTaxi.get(node);
            List<RoboTaxi> RebCarsAtNode = RebalanceRoboTaxi.get(node);
            List<RoboTaxi> SOCarsAtNode = SORoboTaxi.get(node);
            List<RoboTaxi> DOCarsAtNode = DORoboTaxi.get(node);

            if (StayCarsAtNode.isEmpty() == true) {
                numberStay = 0;
            } else {
                numberStay = StayCarsAtNode.size();
            }

            for (int t = 0; t < PlanningHorizon; t++) {
                if (RebCarsAtNode.isEmpty() == true) {
                    numberReb = 0;
                } else {
                    numberReb = getNumberCarsAbailableAtTime(Time, t, RebCarsAtNode);
                }

                if (SOCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getNumberCarsAbailableAtTime(Time, t, SOCarsAtNode);
                }
                
                if (DOCarsAtNode.isEmpty() == true) {
                    numberDO = 0;
                } else {
                    numberDO = getNumberCarsAbailableAtTime(Time, t, DOCarsAtNode);
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

    private static int getNumberCarsAbailableAtTime(double Time, int t, List<RoboTaxi> carsAtNode) {
        int numberCars = 0;
        for (RoboTaxi car : carsAtNode) {
            Collection<RoboTaxiPlanEntry> plansCollection = car.getCurrentPlans(Time).getPlans().values();
            for (RoboTaxiPlanEntry planEntry : plansCollection) {
                if (planEntry.endTime > Time + t * 5 * 60 && planEntry.endTime <= Time + (t + 1) * 5 * 60
                        && car.getStatus() == planEntry.status) {
                    numberCars = numberCars + 1;
                }
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
}
