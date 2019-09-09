/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

public class TripEndTimeFilter implements Predicate<TaxiTrip> {
    private final Scalar maxEndTime;

    public TripEndTimeFilter(Scalar maxEndTime) {
        GlobalAssert.that(Scalars.lessEquals(Quantity.of(0, "s"), maxEndTime));
        this.maxEndTime = maxEndTime;
    }

    @Override
    public boolean test(TaxiTrip t) {
        // seconds of day when trip starts
        // 15 minutes are added because trips are always projected to start of 15 min interval
        double secStart = 15 * 60.0 + t.pickupDate.getHour() * 3600.0 + //
                t.pickupDate.getMinute() * 60.0 + t.pickupDate.getSecond();

        // end time
        Scalar endTime = Quantity.of(secStart, "s").add(t.duration);

        // trips which end after the maximum end time are rejected
        boolean afterEnd = Scalars.lessEquals(endTime, maxEndTime);
        if (!afterEnd) {
            System.out.println("Trip removed because it ends after the simulation termination: ");
            System.out.println(t.pickupDate + " with duration " + t.duration);
            System.out.println("===");
        }
        return afterEnd;
    }
}
