package amod.dispatcher.claudioForDejan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;

public class CarLinkSelectorClaudioForDejan {
    private Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles;

    /* package */ public CarLinkSelectorClaudioForDejan(Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles) {
        this.availableVehicles = availableVehicles;
    }

    /* package */ Map<RoboTaxi, Link> getRebalanceCommands(VirtualLink<Link> vLink, Integer numReb) {

        Map<RoboTaxi, Link> rebalanceCommands = new HashMap<>();

        VirtualNode<Link> fromNode = vLink.getFrom();
        List<RoboTaxi> avTaxis = availableVehicles.get(fromNode);

        List<RoboTaxi> rebalanceTaxis = new ArrayList<>();

        for (int i = 0; i < numReb; ++i) {
            rebalanceTaxis.add(avTaxis.get(i)); // TODO what if not enough? How to ensure each
                                                // picked only once?
        }

        for (RoboTaxi robotaxi : rebalanceTaxis) {
            Link rebalanceToLink = vLink.getTo().getLinks().iterator().next(); // TODO where do you
                                                                               // want to send them?
            rebalanceCommands.put(robotaxi, rebalanceToLink);
        }

        return rebalanceCommands;

    }
}
