package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;

public class RebalanceCarSelector {
    private List<double[]> controlLaw;
    
    
    public RebalanceCarSelector(List<double[]> controlLaw) {
        this.controlLaw = controlLaw;
    }
    
    public List<Pair<RoboTaxi, Link>> getRebalanceCommands(VirtualNode<Link> from, Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles, VirtualNetwork<Link> virtualNetwork) throws Exception {

        List<RoboTaxi> avTaxis = availableVehicles.get(from);
        int indexFromNode = from.getIndex();
        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();
        double[] controlInput = controlLaw.get(indexFromNode);
        
        if(controlInput.equals(ArrayUtils.EMPTY_DOUBLE_ARRAY)) {
            return null;
        }
        
        if(avTaxis.isEmpty()) {
            return null;
        }
        
        
        int iteration = 0;
        List<Integer> removeElements = new ArrayList<Integer>();
        for (double node : controlInput) {
            node = node - 1;
            int indexNode = (int) node;
            
            if(indexNode<1) {
                iteration = iteration + 1;
                continue;
            }

            if (avTaxis.isEmpty()) {
                break;
            }

            RoboTaxi nextRoboTaxi = avTaxis.get(0);
            avTaxis.remove(nextRoboTaxi);
            VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
            Optional<Link> linkOption = toNode.getLinks().stream().findAny();

            Pair<RoboTaxi, Link> xZOCommands = Pair.of(nextRoboTaxi, linkOption.get());
            rebalanceCommandsList.add(xZOCommands);
            removeElements.add(iteration);
            iteration = iteration + 1;

        }
        
        if(!removeElements.isEmpty()) {
            for(int removeArray: removeElements) {
                controlInput[removeArray] = 0;
            }

            controlLaw.set(indexFromNode, controlInput);
        }
        
        if(avTaxis.size() >= controlInput.length) {
            int car = 0;
            for(double node: controlInput) {
                node = node - 1;
                if(node < 0) {
                    throw new Exception("Node ID cannot be negative");
                }
                RoboTaxi RoboTaxi = avTaxis.get(car);
                VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                Pair<RoboTaxi, Link> rebalanceCommands = Pair.of(RoboTaxi, linkOption.get());
                rebalanceCommandsList.add(rebalanceCommands);
                car = car + 1;
            }
            controlInput = ArrayUtils.EMPTY_DOUBLE_ARRAY;
            controlLaw.set(indexFromNode, controlInput);
            return rebalanceCommandsList;
        }
        else if(avTaxis.size() < controlInput.length) {
            double node;
            for(int i=0; i<avTaxis.size(); ++i) {
                node = controlInput[0] - 1;
                if(node < 0) {
                    throw new Exception("Node ID cannot be negative");
                }
                RoboTaxi roboTaxi = avTaxis.get(i);
                VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                Pair<RoboTaxi, Link> rebalanceCommands = Pair.of(roboTaxi, linkOption.get());
                rebalanceCommandsList.add(rebalanceCommands);
                ArrayUtils.remove(controlInput, 0);
                
            }
                
            System.out.println("Not enough cars for control law");
            controlLaw.set(indexFromNode, controlInput);
            return rebalanceCommandsList;
        }
        else {
            return null;
        }
       

    }

}
