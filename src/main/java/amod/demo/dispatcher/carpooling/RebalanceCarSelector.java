//package amod.demo.dispatcher.carpooling;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.matsim.api.core.v01.network.Link;
//
//import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
//import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
//
//public class RebalanceCarSelector {
//    private List<double[]> controlLaw;
//
//    public RebalanceCarSelector(List<double[]> controlLaw) {
//        this.controlLaw = controlLaw;
//    }
//
//    public List<Pair<RoboTaxi, Link>> getRebalanceCommands(VirtualNode<Link> from,
//            Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles, VirtualNetwork<Link> virtualNetwork,
//            List<Link> linkList, Collection<RoboTaxi> emptyDrivingVehicles, int maxDrivingEmptyCars) throws Exception {
//
//        List<RoboTaxi> avTaxis = availableVehicles.get(from);
//        int indexFromNode = from.getIndex();
//        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();
//        double[] controlInput = controlLaw.get(indexFromNode);
//
//        int numberAssignedCars = 0;
//        boolean rebalanceFlag = false;
//        List<RoboTaxi> rebalancingCars = new ArrayList<RoboTaxi>();
//        List<RoboTaxi> findRoboTaxi = new ArrayList<RoboTaxi>();
//
//        if (Arrays.stream(controlInput).sum() == 0) {
//            return null;
//        }
//
//        if (avTaxis.isEmpty()) {
//            return null;
//        }
//
//        int iteration = 0;
//        List<Integer> removeElements = new ArrayList<Integer>();
//        for (double node : controlInput) {
//            node = node - 1;
//            int indexNode = (int) node;
//
//            if (indexNode < 0) {
//                iteration = iteration + 1;
//                continue;
//            }
//
//            if (avTaxis.isEmpty()) {
//                break;
//            }
//
//            if (emptyDrivingVehicles.size() + numberAssignedCars >= maxDrivingEmptyCars) {
//                rebalancingCars = avTaxis.stream()
//                        .filter(car -> !car.getMenu().getCourses().isEmpty()
//                                && car.getMenu().getStarterCourse().getMealType().equals(SharedMealType.REDIRECT))
//                        .collect(Collectors.toList());
//                if (rebalancingCars.isEmpty()) {
//                    break;
//                }
//                rebalanceFlag = true;
//            }
//
//            if (rebalanceFlag) {
//                findRoboTaxi = rebalancingCars;
//            } else {
//                findRoboTaxi = avTaxis;
//                numberAssignedCars = numberAssignedCars + 1;
//            }
//
//            RoboTaxi nextRoboTaxi = findRoboTaxi.get(0);
//            avTaxis.remove(nextRoboTaxi);
//            availableVehicles.get(from).remove(nextRoboTaxi);
//
//            VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
//            Link rebalanceLink = linkList.get(toNode.getIndex());
//
//            Pair<RoboTaxi, Link> xZOCommands = Pair.of(nextRoboTaxi, rebalanceLink);
//            rebalanceCommandsList.add(xZOCommands);
//            removeElements.add(iteration);
//            iteration = iteration + 1;
//
//        }
//
//        if (!removeElements.isEmpty()) {
//            for (int removeArray : removeElements) {
//                controlInput[removeArray] = 0;
//            }
//
//            controlLaw.set(indexFromNode, controlInput);
//        }
//
//        if (rebalanceCommandsList.isEmpty()) {
//            return null;
//        }
//        return rebalanceCommandsList;
//
//    }
//
//    public List<double[]> getControlLawRebalance() {
//        return controlLaw;
//    }
//
//}
