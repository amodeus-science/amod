/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

public class TripMaxSpeedFilter implements Predicate<TaxiTrip> {
    private final FastLinkLookup fll;
    private final LeastCostPathCalculator lcpc;
    private final Scalar maxAllowedSpeed;
    private int count = 0;

    public TripMaxSpeedFilter(Network network, MatsimAmodeusDatabase db, Scalar maxAllowedSpeed) {
        this.maxAllowedSpeed = maxAllowedSpeed;
        // least cost path calculator
        lcpc = new FastAStarLandmarksFactory()//
                .createPathCalculator(network, new DistanceAsTravelDisutility(), //
                        new FreeSpeedTravelTime());
        // fast link lookup
        fll = new FastLinkLookup(network, db);
    }

    @Override
    public boolean test(TaxiTrip trip) {
        // place my code here
        Link origin = fll.getLinkFromWGS84(TensorCoords.toCoord(trip.pickupLoc));
        Link destin = fll.getLinkFromWGS84(TensorCoords.toCoord(trip.dropoffLoc));

        // the time value is set to 1 (arbitrary choice) as only the distance matters
        // for the optimization metric
        Path shortest = lcpc.calcLeastCostPath(origin.getFromNode(), destin.getToNode(), 1, null, null);
        double dSum = 0.0;
        for (Link link : shortest.links)
            dSum += link.getLength();

        Scalar distance = Quantity.of(dSum, "m");

        // previous filter must enure that trip duration > 0
        GlobalAssert.that(Scalars.lessThan(Quantity.of(0, "s"), trip.duration));
        Scalar speed = distance.divide(trip.duration);
        if (Scalars.lessThan(maxAllowedSpeed, speed)) {
            ++count;
            // System.out.println("distance: " + distance);
            // System.out.println("duration: " + trip.duration);
            System.out.println("Total mph removed:      " + count);
            System.out.println("mph too high:       " + speed.multiply(Quantity.of(2.2369363, "miles *h^-1*s*m^-1")));
            return false;
        }
        return true;
    }
}
