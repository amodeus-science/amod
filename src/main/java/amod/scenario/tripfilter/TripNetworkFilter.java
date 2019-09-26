/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.est.DurationCompare;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;

/** This filter calculates the min-time-path in the network without traffic.
 * Trips with a duration that is smaller than this value are rejected.
 * Trips which are not above a certain minimum average speed are rejected. */
public class TripNetworkFilter implements Predicate<TaxiTrip> {

    private final ShortestDurationCalculator calc;
    private final Scalar maxDelay;
    private final Scalar minSpeed;
    private final Scalar minDistance;

    public TripNetworkFilter(Network network, MatsimAmodeusDatabase db, //
            Scalar minSpeed, Scalar maxDelay, Scalar minDistance) {
        calc = new ShortestDurationCalculator(network, db);
        this.maxDelay = maxDelay;
        this.minSpeed = minSpeed;
        this.minDistance = minDistance;
    }

    @Override
    public boolean test(TaxiTrip trip) {

        /** getting the data */
        DurationCompare compare = new DurationCompare(trip, calc);

        /** evaluating criteria */
        boolean slowerThanNetwork = Scalars.lessEquals(compare.nwPathDurationRatio, RealScalar.ONE);
        boolean belowMaxDelay = Scalars.lessEquals(trip.duration.subtract(compare.pathTime), maxDelay);
        boolean fasterThanMinSpeed = Scalars.lessEquals(minSpeed, compare.pathDist.divide(trip.duration));
        boolean longerThanMinDistance = Scalars.lessEquals(minDistance, compare.pathDist);
        boolean hasRealPath = compare.path.links.size() > 1;

        /** return true if all ok */
        return slowerThanNetwork && belowMaxDelay && fasterThanMinSpeed && longerThanMinDistance && hasRealPath;
    }
}
