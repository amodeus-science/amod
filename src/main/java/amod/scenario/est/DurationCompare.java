/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

/** Helper class to compare the duration of a taxi trip (from live data)
 * and a network path. If the nwPathdurationRatio is larger than 1,
 * the trip is slower in the simulatio network than in the original data. */
public class DurationCompare {

    public final Path path;
    public final Scalar duration;
    public final Scalar pathTime;
    /** =1 simulation duration identical t.recorded duration
     * < 1 simulation duration faster than recorded duration
     * > 1 simulation duration slower than recorded duration */
    public final Scalar nwPathDurationRatio;

    public DurationCompare(TaxiTrip trip, ShortestDurationCalculator calc) {
        path = calc.computePath(trip);
        pathTime = Quantity.of(path.travelTime, "s");
        duration = trip.duration;
        nwPathDurationRatio = pathTime.divide(duration);
    }
}
