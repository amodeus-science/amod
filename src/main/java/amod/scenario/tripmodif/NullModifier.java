/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;

import org.apache.commons.io.FileUtils;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;

public class NullModifier implements TaxiDataModifier {

    @Override
    public File modify(File taxiData, MatsimAmodeusDatabase db) throws Exception {
        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
        FileUtils.copyFile(taxiData, outFile);        
        return outFile;
    }
}
