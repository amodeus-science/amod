/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ch.ethz.idsc.amodeus.util.io.Unzip;
import ch.ethz.idsc.tensor.io.HomeDirectory;

enum UnzipDemo {
    ;
    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = HomeDirectory.file("aido-scenario.zip");
        Unzip.of(file, HomeDirectory.file("SCEN_OUT"), true);
    }
}
