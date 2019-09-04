/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import ch.ethz.idsc.amodeus.util.TaxiTrip;

/* package */ class TripDateFilter implements Predicate<TaxiTrip> {
    private final LocalDateTime date;

    public TripDateFilter(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public boolean test(TaxiTrip trip) {
        return StaticHelper.sameDay(date, trip.pickupDate) && StaticHelper.sameDay(date, trip.dropoffDate);
    }
}
