/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

/* package */ enum StaticHelper {
    ;

    public static int endTime(TaxiTrip trip) {

        return trip.pickupDate.getHour() * 3600//
                + trip.pickupDate.getMinute() * 60//
                + trip.pickupDate.getSecond()//
                + trip.duration.number().intValue();

    }

}
