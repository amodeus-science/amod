package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    
    List<Pair<RoboTaxi, Link>> getRebalanceCommands(VirtualNode<Link> from, Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles, VirtualNetwork<Link> virtualNetwork, List<Pair<Integer, Link>> listpair) throws Exception {

        List<RoboTaxi> avTaxis = availableVehicles.get(from);
        int indexFromNode = from.getIndex();
        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();
        double[] controlInput = controlLaw.get(indexFromNode);
        
        if(avTaxis.isEmpty() == false && avTaxis.size() >= controlInput.length) {
            int car = 0;
            for(double node: controlInput) {
                node = node - 1;
                if(node < 0) {
                    return null;
                }
                RoboTaxi RoboTaxi = avTaxis.get(car);
                VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                Pair<RoboTaxi, Link> rebalanceCommands = Pair.of(RoboTaxi, getLink(toNode.getIndex(), listpair));
                rebalanceCommandsList.add(rebalanceCommands);
                car = car + 1;
            }
            controlInput = ArrayUtils.EMPTY_DOUBLE_ARRAY;
            controlLaw.set(indexFromNode, controlInput);
            return rebalanceCommandsList;
        }
        else if(avTaxis.isEmpty() == false && avTaxis.size() < controlInput.length) {
            double node;
            for(int i=0; i<avTaxis.size(); ++i) {
                node = controlInput[0] - 1;
                if(node < 0) {
                    return null;
                }
                RoboTaxi roboTaxi = avTaxis.get(i);
                VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                Pair<RoboTaxi, Link> rebalanceCommands = Pair.of(roboTaxi, getLink(toNode.getIndex(), listpair));
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
    
    private static Link getLink(int node, List<Pair<Integer, Link>> listpair) throws Exception {
        for(Pair<Integer, Link> pair: listpair)
            if(pair.getLeft()==node)
                return pair.getRight();
        throw new Exception("No equal node");
    }

}
