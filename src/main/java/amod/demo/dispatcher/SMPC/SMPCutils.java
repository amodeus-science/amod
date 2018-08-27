package amod.demo.dispatcher.SMPC;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.router.AVRouter;

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
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi, VirtualNetwork<Link> virtualNetwork, ParallelLeastCostPathCalculator router) {
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
                    numberReb = getNumberOfArrivingCars(time, t, timeStep, rebalanceCarsAtNode, router);
                }

                if (soCarsAtNode.isEmpty() == true) {
                    numberSO = 0;
                } else {
                    numberSO = getNumberOfArrivingCars(time, t, timeStep, soCarsAtNode, router);
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

private static int getNumberOfArrivingCars(double now, int t, int timeStep, List<RoboTaxi> roboTaxiList, ParallelLeastCostPathCalculator router) {
        
        int numberCars = 0;
        for (RoboTaxi roboTaxi : roboTaxiList) {
            Link toLink = roboTaxi.getCurrentDriveDestination();
            Link fromLink = roboTaxi.getLastKnownLocation();
            Scalar time = Quantity.of(now, SI.SECOND);
            Scalar arrivingTime = timeFromTo(fromLink, toLink, time, roboTaxi, router);
            if (arrivingTime.number().doubleValue() > now + t * timeStep * 60
                    && arrivingTime.number().doubleValue() <= now + (t + 1) * timeStep * 60) {
                numberCars = numberCars + 1;
            }
        }

        return numberCars;
    }

/** @return time in seconds needed for {@link RoboTaxi} @param roboTaxi to travel from {@link Link}
 * @param from to the {@link Link} @param to starting at {@link Scalar} @param now and using
 *            the {@link AVRouter} @param router
 * @return null if path calculation unsuccessful */
private static Scalar timeFromTo(Link from, Link to, Scalar now, RoboTaxi roboTaxi, ParallelLeastCostPathCalculator router) {
    Future<Path> path = router.calcLeastCostPath(from.getFromNode(), to.getToNode(), now.number().doubleValue(), //
            null, null);
    Double travelTime = null;
    try {
        travelTime = path.get().travelTime;
    } catch (Exception e) {
        System.err.println("Calculation of expected arrival failed.");
    }
    return Quantity.of(travelTime, SI.SECOND);
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
