/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.create.LinkSpeedsExport;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.io.SaveFormats;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ enum StaticHelper {
    ;

    public static int startTime(TaxiTrip trip) {
        return trip.pickupDate.getHour() * 3600//
                + trip.pickupDate.getMinute() * 60//
                + trip.pickupDate.getSecond();

    }

    public static int endTime(TaxiTrip trip) {
        return startTime(trip) + trip.duration.number().intValue();
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

    public static void exportRatioMap(Map<TaxiTrip, Scalar> ratioLookupMap, String append) {
        Tensor all = Tensors.empty();
        ratioLookupMap.values().forEach(s -> all.append(s));
        try {
            SaveFormats.MATHEMATICA.save(all, new File("/home/clruch/Downloads/"), "diff" + append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
