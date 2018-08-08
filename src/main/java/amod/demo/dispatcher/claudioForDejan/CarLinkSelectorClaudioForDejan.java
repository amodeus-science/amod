package amod.demo.dispatcher.claudioForDejan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlanEntry;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;

public class CarLinkSelectorClaudioForDejan {
    private Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles;

    /* package */ public CarLinkSelectorClaudioForDejan(Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles) {
        this.availableVehicles = availableVehicles;
    }

    /* package */ List<Pair<RoboTaxi, Link>> getRebalanceCommands(VirtualNode<Link> from, double[] controlLaw, VirtualNetwork<Link> virtualNetwork, List<Pair<Integer, Link>> listpair) throws Exception {

        List<RoboTaxi> avTaxis = availableVehicles.get(from);
        List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();
        
        if(avTaxis.isEmpty() == false && avTaxis.size() >= controlLaw.length) {
            int car = 0;
            for(double node: controlLaw) {
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
            return rebalanceCommandsList;
        }
        else if(avTaxis.isEmpty() == false && avTaxis.size() < controlLaw.length) {
            double node;
            for(int i=0; i<avTaxis.size(); ++i) {
                node = controlLaw[i] - 1;
                if(node < 0) {
                    return null;
                }
                RoboTaxi roboTaxi = avTaxis.get(i);
                VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                Pair<RoboTaxi, Link> rebalanceCommands = Pair.of(roboTaxi, getLink(toNode.getIndex(), listpair));
                rebalanceCommandsList.add(rebalanceCommands);
            }
                
//            throw new Exception("Not enough cars for control law");
            System.out.println("Not enough cars for control law");
            return rebalanceCommandsList;
        }
//        else if(avTaxis.isEmpty() == true && controlLaw[0] != 0) {
//            throw new Exception("No cars for control law");
//        }
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
