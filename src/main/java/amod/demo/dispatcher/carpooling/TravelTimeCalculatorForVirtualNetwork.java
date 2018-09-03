package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.router.AVRouter;

public enum TravelTimeCalculatorForVirtualNetwork {
    ;
    
static Map<VirtualLink<Link>,Double> computeTravelTimes(Collection<VirtualLink<Link>> vLinks, Scalar now, AVRouter router){
        
        Map<VirtualLink<Link>,Double> tTimes = new HashMap<>();
        
        for(VirtualLink<Link> vLink : vLinks){
            VirtualNode<Link> fromNode = vLink.getFrom();
            VirtualNode<Link> toNode = vLink.getTo();
            
            Set<Link> linkSetFrom = fromNode.getLinks();
            List<Link> linkListFrom = new ArrayList<Link>(linkSetFrom);
            List<Link> linkListFilteredFrom = linkListFrom.stream().filter(link -> link!=null).collect(Collectors.toList());
            Link linkFrom = linkListFilteredFrom.get(new Random().nextInt(linkListFilteredFrom.size()-1));
            
            Set<Link> linkSetTo = toNode.getLinks();
            List<Link> linkListTo = new ArrayList<Link>(linkSetTo);
            List<Link> linkListFilteredTo = linkListTo.stream().filter(link -> link!=null).collect(Collectors.toList());
            Link linkTo = linkListFilteredTo.get(new Random().nextInt(linkListFilteredTo.size()-1));
            
            Scalar travelTime = timeFromTo(linkFrom, linkTo, now, router);
            
            tTimes.put(vLink, travelTime.number().doubleValue());
        }
        
        return tTimes;
        
        
    }

/** @return time in seconds needed for {@link RoboTaxi} @param roboTaxi to travel from {@link Link}
 * @param from to the {@link Link} @param to starting at {@link Scalar} @param now and using
 *            the {@link AVRouter} @param router
 * @return null if path calculation unsuccessful */
private static Scalar timeFromTo(Link from, Link to, Scalar now, AVRouter router) {
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

}
