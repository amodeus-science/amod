package amod.demo.dispatcher.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;

public class RControl {
    private Tensor controlLaw;

    public RControl(Tensor controlLaw) {
        this.controlLaw = controlLaw;
    }

    public List<Pair<RoboTaxi, Link>> getRebalanceCommands(Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles, VirtualNetwork<Link> virtualNetwork,
            List<Link> linkList, Collection<RoboTaxi> emptyDrivingVehicles, int maxDrivingEmptyCars) throws Exception {

        Collection<VirtualLink<Link>> vLinks = virtualNetwork.getVirtualLinks();
        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();

        for(VirtualLink<Link> vLink: vLinks) {
            VirtualNode<Link> fromNode = vLink.getFrom();
            VirtualNode<Link> toNode = vLink.getTo();
            List<RoboTaxi> avTaxis = availableVehicles.get(fromNode);            
            double controlInput = controlLaw.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();
            int numberAssignedCars = 0;
            boolean rebalanceFlag = false;
            List<RoboTaxi> rebalancingCars = new ArrayList<RoboTaxi>();
            List<RoboTaxi> findRoboTaxi = new ArrayList<RoboTaxi>();
            
            if (controlInput == 0) {
                continue;
            }

            if (avTaxis.isEmpty()) {
                continue;
            }
            
            int iteration = 0;
            
            for(int i=1; i<=controlInput; i++) {
                if (avTaxis.isEmpty()) {
                    break;
                }
                
                if (emptyDrivingVehicles.size() + numberAssignedCars >= maxDrivingEmptyCars) {
                    rebalancingCars = avTaxis.stream()
                            .filter(car -> !car.getMenu().getCourses().isEmpty()
                                    && car.getMenu().getStarterCourse().getMealType().equals(SharedMealType.REDIRECT))
                            .collect(Collectors.toList());
                    if (rebalancingCars.isEmpty()) {
                        break;
                    }
                    rebalanceFlag = true;
                }
                
                if (rebalanceFlag) {
                    findRoboTaxi = rebalancingCars;
                } else {
                    findRoboTaxi = avTaxis;
                    numberAssignedCars = numberAssignedCars + 1;
                }
                
                RoboTaxi nextRoboTaxi = findRoboTaxi.get(0);
                avTaxis.remove(nextRoboTaxi);
                availableVehicles.get(fromNode).remove(nextRoboTaxi);

                Link rebalanceLink = linkList.get(toNode.getIndex());

                Pair<RoboTaxi, Link> xZOCommands = Pair.of(nextRoboTaxi, rebalanceLink);
                rebalanceCommandsList.add(xZOCommands);
                
                iteration = i;
  
            }
            
            RealScalar newControlLaw = DoubleScalar.of(controlInput-iteration);
            
            controlLaw.set(newControlLaw, fromNode.getIndex(), toNode.getIndex());
            
        }
        
        return rebalanceCommandsList;

    }

    public Tensor getControlLawRebalance() {
        return controlLaw;
    }

}
