/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.time.LocalDateTime;
import java.util.Random;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.LocalDateTimes;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

public class TripStartTimeShiftResampling implements TripModifier {

    private final Random random;
    private final Scalar maxShift;

    public TripStartTimeShiftResampling(Random random, Scalar maxShift) {
        this.random = random;
        this.maxShift = maxShift;
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
//        Scalar maxShift = maxShift(duration);

        /** compute a random time shift */
        Scalar shift = RealScalar.of(random.nextDouble()).multiply(maxShift);

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

//    /** @return given a trip duration and start time at a 15 minute interval,
//     *         0 min, 15 min, 30 min, ... and a trip duration @param duration, this returns
//     *         the maximum possible shift in seconds such that the trip still ends in the
//     *         same interval than originally */
//    private Scalar maxShift(Scalar duration) {
//        GlobalAssert.that(Scalars.lessEquals(Quantity.of(0, "s"), duration));
//        int durationSec = duration.number().intValue();
//        int excess = durationSec - (durationSec / 900) * 900;
//        int maxShift = 900 - excess;
//        return Quantity.of(maxShift, "s");
//    }

    @Override
    public void notify(TaxiTrip taxiTrip) {
        // -- deliberately empty
    }
}