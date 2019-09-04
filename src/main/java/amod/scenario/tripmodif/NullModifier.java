/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;

import org.apache.commons.io.FileUtils;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;

public class NullModifier implements TaxiDataModifier {

    @Override
    public File modify(File taxiData) throws Exception {

        /** fast previous version, do again */
        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
        FileUtils.copyFile(taxiData, outFile);
        return outFile;

        // this works as well, TODO remove eventually...
        // Stream<TaxiTrip> originals =ImportTaxiTrips.fromFile(taxiData);
        // File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
        // ExportTaxiTrips.toFile(originals, outFile);
        // return outFile;

    }
}
