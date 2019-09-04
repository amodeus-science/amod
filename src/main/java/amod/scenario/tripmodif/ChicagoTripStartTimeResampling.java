/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.time.LocalDateTime;
import java.util.Random;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.LocalDateTimes;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoTripStartTimeResampling implements TripModifier {

    private final Random random;

    public ChicagoTripStartTimeResampling(Random random) {
        this.random = random;
    }

    @Override
    public TaxiTrip modify(TaxiTrip taxiTrip) {
        TaxiTrip tripOrig = taxiTrip;

        /** get start time and duration */
        LocalDateTime start = tripOrig.pickupDate;
        Scalar duration = tripOrig.duration;

        /** assert that start time is multiple of 15 minutes */
        GlobalAssert.that(start.getMinute() % 15 == 0); // this is always true for Chicago online data

        /** get the maximum time shift to have star and end in
         * the same 15 minute interval */
        int maxShift = maxShift(duration);

        /** compute a random time shift */
        Scalar shift = Quantity.of(random.nextDouble() * maxShift, "s");

        /** compute updated trip and return */
        return TaxiTrip.of(//
                tripOrig.localId, //
                tripOrig.taxiId, //
                tripOrig.pickupLoc, //
                tripOrig.dropoffLoc, //
                tripOrig.distance, //
                tripOrig.waitTime, //
                LocalDateTimes.addTo(tripOrig.pickupDate, shift), //
                tripOrig.duration);
    }

    private int maxShift(Scalar duration) {
        GlobalAssert.that(Scalars.lessEquals(Quantity.of(0, "s"), duration));
        int durationSec = duration.number().intValue();
        int excess = durationSec - (durationSec / 900) * 900;
        int maxShift = 900 - excess;
        return maxShift;
    }

}

// private final double minuteResolution;
//
// public TripStartTimeResampling(double minuteResolution) {
// this.minuteResolution = minuteResolution;
// }
//
// public Stream<TaxiTrip> filter(Stream<TaxiTrip> stream, ScenarioOptions simOptions, Network network) {
// return stream.peek(trip -> {
// int offsetSec = RandomVariate.of(UniformDistribution.of(-30 * minuteResolution, 30 * minuteResolution)).number().intValue();
// LocalDateTime pickupPrev = trip.pickupDate;
// LocalDateTime pickupModi = LocalDateTime.of(pickupPrev.getYear(), pickupPrev.getMonth(), //
// pickupPrev.getDayOfMonth(), pickupPrev.getHour(), pickupPrev.getMinute(), pickupPrev.getSecond() + offsetSec);
// trip.pickupDate = pickupModi;
//
// });
// }