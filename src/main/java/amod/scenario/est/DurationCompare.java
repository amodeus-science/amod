/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ class DurationCompare {

    public final Path path;
    public final Scalar duration;
    public final Scalar pathTime;
    public final Scalar nwPathDurationRatio; // this is < 1 as long as not enough congestion

    public DurationCompare(TaxiTrip trip, ShortestDurationCalculator calc) {
        path = calc.computePath(trip);
        pathTime = Quantity.of(path.travelTime, "s");
        duration = trip.duration;
        nwPathDurationRatio = pathTime.divide(duration);
    }
}
