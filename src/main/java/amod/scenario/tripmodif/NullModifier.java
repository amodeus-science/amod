/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ExportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.ImportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

public class NullModifier implements TaxiDataModifier {

    @Override
    public File modify(File taxiData, MatsimAmodeusDatabase db) throws Exception {

        Stream<TaxiTrip> originals =ImportTaxiTrips.fromFile(taxiData);
        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
        ExportTaxiTrips.toFile(originals, outFile);
        
        return outFile;
        
        
        
        
        /** fast previous version, do again */
//        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
//        FileUtils.copyFile(taxiData, outFile);        
//        return outFile;
    }
}
