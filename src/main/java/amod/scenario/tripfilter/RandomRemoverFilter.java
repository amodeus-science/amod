/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

public class RandomRemoverFilter implements Predicate<TaxiTrip> {

    private final Random random;
    private final double keepShare;

    public RandomRemoverFilter(Random random, double keepShare) {
        this.random = Objects.requireNonNull(random);
        this.keepShare = keepShare;
    }

    @Override
    public boolean test(TaxiTrip t) {        
        if (random.nextDouble() <= keepShare)
            return true;
        return false;
    }
}
