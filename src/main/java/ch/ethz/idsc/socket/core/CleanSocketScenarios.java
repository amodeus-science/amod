/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.io.File;
import java.util.Arrays;

/* package */ enum CleanSocketScenarios {
    ;

    /** clean up downloaded files for aido scenaros */
    public static void now() {
        String[] files = new String[] { "AmodeusOptions.properties", "LPOptions.properties", //
                "av.xml", "matsimConfig.xml", "personAtrributes-with-subpopulation.xml", //
                "preparedNetwork.xml.gz", "preparedNetwork.xml", "config_full.xml", //
                "linkSpeedData", "config.xml", "preparedConfig.xml", "preparedPopulation.xml", //
                "preparedPopulation.xml.gz", "population.xml", "population.xml.gz", //
                "rawPopulation.xml.gz", "network.xml.gz", "rawNetwork.xml.gz", "rawFacilities.xml.gz" };
        Arrays.stream(files).map(File::new).filter(File::exists).forEach(file -> {
            if (!file.delete())
                System.out.println("file: " + file.getName() + " not deleted.");
        });
    }
}
