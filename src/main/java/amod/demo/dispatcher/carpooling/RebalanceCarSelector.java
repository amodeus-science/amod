package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        List<RoboTaxi> avTaxisRedirect = availableVehicles.get(from);
        List<RoboTaxi> avTaxis = avTaxisRedirect.stream().filter(car -> !car.getMenu().hasStarter()).collect(Collectors.toList());
        int indexFromNode = from.getIndex();
        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();
        double[] controlInput = controlLaw.get(indexFromNode);
        
        if(Arrays.stream(controlInput).sum()==0) {
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
            
            if(indexNode<0) {
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
        
        
        if(rebalanceCommandsList.isEmpty()) {
            return null;
        }
        return rebalanceCommandsList;
       

    }

}
