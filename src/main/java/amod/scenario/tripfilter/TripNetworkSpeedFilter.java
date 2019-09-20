/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

/** This filter calculates the min-time-path in the network without traffic.
 * Trips with a duration that is smaller than this value are rejected.
 * Trips which are not above a certain minimum average speed are rejected. */
public class TripNetworkSpeedFilter implements Predicate<TaxiTrip> {

    private final ShortestDurationCalculator calc;
    private final Scalar maxDelay;
    private final Scalar minSpeed;

    public TripNetworkSpeedFilter(Network network, MatsimAmodeusDatabase db, //
            Scalar minSpeed, Scalar maxDelay) {
        calc = new ShortestDurationCalculator(network, db);
        this.maxDelay = maxDelay;
        this.minSpeed = minSpeed;
    }

    @Override
    public boolean test(TaxiTrip trip) {
        Path freeFlowpath = calc.computePath(trip);
        Scalar freeFlowTime = Quantity.of(freeFlowpath.travelTime, "s");
        Scalar dist = Quantity.of(freeFlowpath.links.stream().mapToDouble(l -> l.getLength()).sum(), "m");

        boolean fasterThanMin = Scalars.lessEquals(minSpeed, dist.divide(trip.duration));
        boolean fasterThanNetwork = Scalars.lessEquals(trip.duration, freeFlowTime);
        boolean aboveMaxDelay = Scalars.lessEquals(maxDelay, trip.duration.subtract(freeFlowTime));

        // reject if trip is faster than network allows for or above maximum tolerated delay
        return !fasterThanNetwork && !aboveMaxDelay && fasterThanMin;
    }
}
