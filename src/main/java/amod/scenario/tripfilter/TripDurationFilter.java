/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

/** Filter used to remove {@link TaxiTrip}s which a duration not in the
 * interval [minDuration,maxDuration] */
public class TripDurationFilter implements Predicate<TaxiTrip> {
    private final Scalar minDuration;
    private final Scalar maxDuration;

    public TripDurationFilter(Scalar minDuration, Scalar maxDuration) {
        GlobalAssert.that(Scalars.lessEquals(Quantity.of(0, "s"), minDuration));
        GlobalAssert.that(Scalars.lessEquals(Quantity.of(0, "s"), maxDuration));
        GlobalAssert.that(Scalars.lessEquals(minDuration, maxDuration));
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    @Override
    public boolean test(TaxiTrip t) {
        return Scalars.lessEquals(minDuration, t.duration) && //
                Scalars.lessEquals(t.duration, maxDuration);
    }

}
