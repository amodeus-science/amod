/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.time.LocalDateTime;
import java.util.Random;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.LocalDateTimes;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;

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

        /** assert that start time is multiple of 15 minutes */
        GlobalAssert.that(start.getMinute() % 15 == 0); // this is always true for Chicago online data

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

    @Override
    public void notify(TaxiTrip taxiTrip) {
        // -- deliberately empty
    }
}