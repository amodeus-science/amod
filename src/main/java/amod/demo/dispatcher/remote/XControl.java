package amod.demo.dispatcher.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.matsim.av.passenger.AVRequest;

public class XControl {
    private Tensor controlLaw;

    public XControl(Tensor controlLaw) {
        this.controlLaw = controlLaw;
    }

    List<Pair<RoboTaxi, AVRequest>> getXCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests, List<Link> linkList,
            Collection<RoboTaxi> emptyDrivingVehicles, int maxDrivingEmptyCars, boolean firstRebalance) throws Exception {

        List<Pair<RoboTaxi, AVRequest>> xCommandsList = new ArrayList<>();
        int numberAssignedCars = 0;
        boolean rebalanceFlag = false;
        List<RoboTaxi> rebalancingCars = new ArrayList<RoboTaxi>();
        List<RoboTaxi> findRoboTaxi = new ArrayList<RoboTaxi>();
        Collection<VirtualNode<Link>> vNodes = virtualNetwork.getVirtualNodes();

        
            for (VirtualNode<Link> fromNode : vNodes) {
                List<RoboTaxi> availableCars = stayRoboTaxi.get(fromNode);
                for(VirtualNode<Link> toNode: vNodes) {
                    
                    if (availableCars.isEmpty()) {
                        continue;
                    }

                    List<AVRequest> fromRequests = virtualNodeAVFromRequests.get(fromNode);
                    if (fromRequests.isEmpty()) {
                        continue;
                    }
                    
                    List<AVRequest> toRequests = virtualNodeAVToRequests.get(toNode);
                    if (toRequests.isEmpty()) {
                        continue;
                    }
                    
                    List<AVRequest> fromToRequest = fromRequests.stream().filter(req -> toRequests.contains(req))
                            .collect(Collectors.toList());

                    double controlX = controlLaw.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();

                    if (controlX == 0) {
                        continue;
                    }
                    
                    int counter = 0;
                    
                    for(int i=1; i<=controlX; i++) {
                          
                        if (fromToRequest.isEmpty()) {
                            break;
                        }
                        if (availableCars.isEmpty()) {
                            break;
                        }
                        
                        if (emptyDrivingVehicles.size() + numberAssignedCars >= maxDrivingEmptyCars) {
                            rebalancingCars = availableCars.stream()
                                    .filter(car -> !car.getUnmodifiableViewOfCourses().isEmpty()
                                            && car.getUnmodifiableViewOfCourses().get(0).getMealType().equals(SharedMealType.REDIRECT))
                                    .collect(Collectors.toList());

                            if (rebalancingCars.isEmpty()) {
                                break;
                            }
                            rebalanceFlag = true;
                        }
                        
                        AVRequest avRequest = fromToRequest.get(0);
                        fromToRequest.remove(avRequest);
                        fromRequests.remove(avRequest);
                        toRequests.remove(avRequest);
                        virtualNodeAVFromRequests.get(fromNode).remove(avRequest);
                        virtualNodeAVToRequests.get(toNode).remove(avRequest);

                        if (rebalanceFlag) {
                            findRoboTaxi = rebalancingCars;
                            rebalanceFlag = false;
                        } else {
                            findRoboTaxi = availableCars;
                            numberAssignedCars = numberAssignedCars + 1;
                        }
                        
                        RoboTaxi closestRoboTaxi;
                        
                        List<RoboTaxi> rebalanceCars = findRoboTaxi.stream().filter(car -> !car.getUnmodifiableViewOfCourses().isEmpty()
    							&& car.getUnmodifiableViewOfCourses().get(0).getMealType() == SharedMealType.REDIRECT)
    							.collect(Collectors.toList());
                        
                        if(!rebalanceCars.isEmpty() && firstRebalance) {
                        	closestRoboTaxi = StaticHelperRemote.findClostestVehicle(avRequest, rebalanceCars);
                        	rebalanceCars.remove(closestRoboTaxi);
                            availableCars.remove(closestRoboTaxi);
                            stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);
                        } else {
                        	closestRoboTaxi = StaticHelperRemote.findClostestVehicle(avRequest, findRoboTaxi);
                            availableCars.remove(closestRoboTaxi);
                            stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);
                        }

                        Pair<RoboTaxi, AVRequest> xZOCommands = Pair.of(closestRoboTaxi, avRequest);
                        xCommandsList.add(xZOCommands);
                        
                        counter = i;
                        
                    }
                    
                    RealScalar newControlLaw = DoubleScalar.of(controlX-counter);
                    
                    controlLaw.set(newControlLaw, fromNode.getIndex(), toNode.getIndex());
                }
                                 
            }
        

        return xCommandsList;
    }

    Tensor getControlLawX() {
        return controlLaw;
    }
    

}
