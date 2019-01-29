package amod.demo.dispatcher.remote;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.matsim.av.router.AVRouter;

public enum ParkingLinkCalculator {
    ;

    public static Link getParkingLink(Link fromLink, VirtualNode<Link> virtualNode, Scalar now, AVRouter router, List<Link> linkList) {

        Set<Link> allLinks = virtualNode.getLinks();

        for (Link link : allLinks) {
            Scalar time = timeFromTo(fromLink, link, now, router);
            if (time.number().doubleValue() >= 2 * 60 && time.number().doubleValue() <= 3 * 60) {
                return link;
            }
        }
        
        return linkList.get(virtualNode.getIndex());
        
    }

    /**
     * @return time in seconds needed for {@link RoboTaxi} @param roboTaxi to
     *         travel from {@link Link}
     * @param from
     *            to the {@link Link} @param to starting at
     *            {@link Scalar} @param now and using the
     *            {@link AVRouter} @param router
     * @return null if path calculation unsuccessful
     */
    private static Scalar timeFromTo(Link from, Link to, Scalar now, AVRouter router) {
        Future<Path> path = router.calcLeastCostPath(from.getFromNode(), to.getToNode(), now.number().doubleValue(), //
                null, null);
        Double travelTime = null;
        try {
            travelTime = path.get().travelTime;
        } catch (Exception e) {
            System.err.println("Calculation of expected arrival failed.");
        }
        if (travelTime == 0) {
            System.out.println("zero travel time!!");
        }
        return Quantity.of(travelTime, SI.SECOND);
    }

}
