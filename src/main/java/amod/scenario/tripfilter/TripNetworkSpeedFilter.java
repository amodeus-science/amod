/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;

/** This filter calculates the min-time-path in the network without traffic.
 * Trips with a duration that is smaller than this value are rejected. */
public class TripNetworkSpeedFilter implements Predicate<TaxiTrip> {

    private final ShortestDurationCalculator calc;
    private final Scalar maxDelay;

    public TripNetworkSpeedFilter(Network network, MatsimAmodeusDatabase db, Scalar maxDelay) {
        calc = new ShortestDurationCalculator(network, db);
        this.maxDelay = maxDelay;
    }

    @Override
    public boolean test(TaxiTrip trip) {
        Scalar freeFlowTime = calc.computeFreeFlowTime(trip);
        boolean fasterThanNetwork = Scalars.lessEquals(trip.duration, freeFlowTime);
        boolean aboveMaxDelay = Scalars.lessEquals(maxDelay, trip.duration.subtract(freeFlowTime));

        // reject if trip is faster than network allows for or above maximum tolerated delay
        return !fasterThanNetwork && !aboveMaxDelay;
    }
}
