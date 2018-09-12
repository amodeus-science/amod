/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ch.ethz.idsc.amodeus.aido.util.Unzip;
import ch.ethz.idsc.amodeus.util.math.UserHome;

enum UnzipDemo {
    ;
    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = UserHome.file("aido-scenario.zip");
        Unzip.of(file, UserHome.file("SCEN_OUT"), true);
    }
}
