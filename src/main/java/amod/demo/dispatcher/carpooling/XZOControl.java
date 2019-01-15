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
//import java.util.stream.Stream;
//
//import org.apache.commons.lang3.tuple.Triple;
//import org.matsim.api.core.v01.network.Link;
//
//import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
//import ch.ethz.idsc.amodeus.dispatcher.shared.SharedCourse;
//import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
//import ch.ethz.matsim.av.passenger.AVRequest;
//
//public class XZOControl {
//    private List<List<double[]>> controlLaw;
//
//    public XZOControl(List<List<double[]>> controlLaw) {
//        this.controlLaw = controlLaw;
//    }
//
//    List<Triple<RoboTaxi, AVRequest, Link>> getXZOCommands(VirtualNetwork<Link> virtualNetwork,
//            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
//            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
//            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests, List<Link> linkList,
//            Collection<RoboTaxi> emptyDrivingVehicles, int maxDrivingEmptyCars) throws Exception {
//
//        List<Triple<RoboTaxi, AVRequest, Link>> xZOCommandsList = new ArrayList<>();
//        int numberAssignedCars = 0;
//        boolean rebalanceFlag = false;
//        List<RoboTaxi> rebalancingCars = new ArrayList<RoboTaxi>();
//        List<RoboTaxi> findRoboTaxi = new ArrayList<RoboTaxi>();
//
//        for (VirtualNode<Link> destinationNode : virtualNetwork.getVirtualNodes()) {
//
//            List<AVRequest> toRequests = virtualNodeAVToRequests.get(destinationNode);
//            if (toRequests.isEmpty()) {
//                continue;
//            }
//            for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
//
//                List<RoboTaxi> availableCars = stayRoboTaxi.get(fromNode);
//
//                if (availableCars.isEmpty()) {
//                    continue;
//                }
//
//                List<AVRequest> fromRequests = virtualNodeAVFromRequests.get(fromNode);
//                if (fromRequests.isEmpty()) {
//                    continue;
//                }
//
//                List<AVRequest> fromToRequest = fromRequests.stream().filter(req -> toRequests.contains(req))
//                        .collect(Collectors.toList());
//
//                if (fromToRequest.isEmpty()) {
//                    continue;
//                }
//
//                double[] controlXzo = controlLaw.get(destinationNode.getIndex()).get(fromNode.getIndex());
//
//                if (Arrays.stream(controlXzo).sum() == 0) {
//                    continue;
//                }
//
//                for (int ixzo = 0; ixzo < controlXzo.length; ixzo++) {
//                    int toNodeRedirect = (int) controlXzo[ixzo] - 1;
//                    if (toNodeRedirect < 0) {
//                        continue;
//                    }
//                    if (fromToRequest.isEmpty()) {
//                        break;
//                    }
//                    if (availableCars.isEmpty()) {
//                        break;
//                    }
//
//                    if (emptyDrivingVehicles.size() + numberAssignedCars >= maxDrivingEmptyCars) {
//                        rebalancingCars = availableCars.stream()
//                                .filter(car -> !car.getMenu().getCourses().isEmpty()
//                                        && car.getMenu().getStarterCourse().getMealType().equals(SharedMealType.REDIRECT))
//                                .collect(Collectors.toList());
//
//                        if (rebalancingCars.isEmpty()) {
//                            break;
//                        }
//                        rebalanceFlag = true;
//                    }
//
//                    AVRequest avRequest = fromToRequest.get(0);
//                    fromToRequest.remove(avRequest);
//                    fromRequests.remove(avRequest);
//                    toRequests.remove(avRequest);
//                    virtualNodeAVFromRequests.get(fromNode).remove(avRequest);
//                    virtualNodeAVToRequests.get(destinationNode).remove(avRequest);
//
//                    if (rebalanceFlag) {
//                        findRoboTaxi = rebalancingCars;
//                        rebalanceFlag = false;
//                    } else {
//                        findRoboTaxi = availableCars;
//                        numberAssignedCars = numberAssignedCars + 1;
//                    }
//                    RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, findRoboTaxi);
//                    availableCars.remove(closestRoboTaxi);
//                    stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);
//
//                    VirtualNode<Link> toVirtualNodeRedirect = virtualNetwork.getVirtualNode(toNodeRedirect);
//                    Link redirectLink = linkList.get(toVirtualNodeRedirect.getIndex());
//
//                    Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest, redirectLink);
//                    xZOCommandsList.add(xZOCommands);
//
//                    removeXZOCommand(destinationNode, fromNode, ixzo);
//
//                }
//            }
//        }
//
//        return xZOCommandsList;
//    }
//
//    List<List<double[]>> getControlLawXZO() {
//        return controlLaw;
//    }
//
//    void removeXZOCommand(VirtualNode<Link> fromNode, VirtualNode<Link> toNode, int toNodeSecond) {
//        controlLaw.get(fromNode.getIndex()).get(toNode.getIndex())[toNodeSecond] = 0;
//    }
//
//}
