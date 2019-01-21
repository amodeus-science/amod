package amod.demo.dispatcher.remote;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.router.AVRouter;

public enum TravelTimeCalculatorVirtualNetwork {
    ;
    
public static Tensor computeTravelTimes(VirtualNetwork<Link> virtualNetwork, Scalar now, AVRouter router, List<Link> linkList){
        
        Tensor tTimes = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        Collection<VirtualLink<Link>> vLinks = virtualNetwork.getVirtualLinks();
        
        for(int i=0; i<virtualNetwork.getvNodesCount(); i++) {
            for(int j=0; j<virtualNetwork.getvNodesCount(); j++) {
                
                Link linkFrom = linkList.get(i);
                GlobalAssert.that(virtualNetwork.getVirtualNode(i).getLinks().contains(linkFrom));
                           
                Link linkTo = linkList.get(j);
                GlobalAssert.that(virtualNetwork.getVirtualNode(j).getLinks().contains(linkTo));
                
                Scalar travelTime = timeFromTo(linkFrom, linkTo, now, router);
                if(i!=j && travelTime.equals(Quantity.of(0, SI.SECOND))) {
                    GlobalAssert.that(false);
                }
                
                tTimes.set(travelTime, i, j);
                
            }
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
    if(travelTime==0) {
        System.out.println("zero travel time!!");
    }
    return Quantity.of(travelTime, SI.SECOND);
}

}
