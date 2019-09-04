/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.util.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;

/** Filter used to remove {@link TaxiTrip}s which a duration longer than
 * maxTripDuration. */
public class TripDurationFilter implements Predicate<TaxiTrip> {
    private final Scalar maxTripDuration;

    public TripDurationFilter(Scalar maxTripDuration) {
        this.maxTripDuration = maxTripDuration;
    }

    @Override
    public boolean test(TaxiTrip t) {
        return Scalars.lessEquals(t.duration, maxTripDuration);
    }

}
