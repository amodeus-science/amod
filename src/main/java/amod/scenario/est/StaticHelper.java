/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.create.LinkSpeedsExport;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

/* package */ enum StaticHelper {
    ;

    public static int endTime(TaxiTrip trip) {
        return trip.pickupDate.getHour() * 3600//
                + trip.pickupDate.getMinute() * 60//
                + trip.pickupDate.getSecond()//
                + trip.duration.number().intValue();

    }

    public static void export(File processingDir, LinkSpeedDataContainer lsData, String nameAdd) {
        /** exporting final link speeds file */
        File linkSpeedsFile = new File(processingDir, "/linkSpeedData" + nameAdd);
        try {
            LinkSpeedsExport.using(linkSpeedsFile, lsData);
        } catch (IOException e) {
            System.err.println("Export of LinkSpeedDataContainer failed: ");
            e.printStackTrace();
        }
    }
}
