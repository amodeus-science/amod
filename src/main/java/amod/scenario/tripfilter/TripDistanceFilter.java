package amod.scenario.tripfilter;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;

public class TripDistanceFilter implements Predicate<TaxiTrip> {

    private final FastLinkLookup fll;
    private final LeastCostPathCalculator lcpc;
    private final Scalar minDistance;

    public TripDistanceFilter(Network network, MatsimAmodeusDatabase db, Scalar minDistance) {
        this.minDistance = minDistance;
        // least cost path calculator
        lcpc = new FastAStarLandmarksFactory()//
                .createPathCalculator(network, new DistanceAsTravelDisutility(), //
                        new FreeSpeedTravelTime());
        // fast link lookup
        fll = new FastLinkLookup(network, db);
    }

    @Override
    public boolean test(TaxiTrip trip) {
        /** get origin and destination */
        Link origin = fll.getLinkFromWGS84(TensorCoords.toCoord(trip.pickupLoc));
        Link destin = fll.getLinkFromWGS84(TensorCoords.toCoord(trip.dropoffLoc));

        /** compute minimal network distance */
        // lcpc.

        // FIXME implement if needed
        GlobalAssert.that(false);
        return true;

    }

}
