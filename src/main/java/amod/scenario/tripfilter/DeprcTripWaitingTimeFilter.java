/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;

/** {@link TaxiTrip} filter used to remove taxi {@link TaxiTrip}s with a maximum wait time
 * which is too long. */
@Deprecated
// TODO refactor and use or delete
/* package */ class DeprcTripWaitingTimeFilter implements Predicate<TaxiTrip> {
    private final Scalar maxWaitTime;

    public DeprcTripWaitingTimeFilter(Scalar maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public boolean test(TaxiTrip t) {
        return Scalars.lessEquals(t.waitTime, maxWaitTime);
    }

}
