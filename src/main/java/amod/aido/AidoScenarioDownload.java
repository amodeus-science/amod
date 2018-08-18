/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import amod.aido.util.ContentType;
import amod.aido.util.HttpDownloader;
import amod.aido.util.Unzip;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.io.ResourceData;

/* package */ enum AidoScenarioDownload {
    ;

    /** @param key for instance "SanFrancisco"
     * @throws IOException */
    /* package */ static void download(String key) throws IOException {

        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        if (properties.containsKey(key)) {
            String value = properties.getProperty(key);
            System.out.println(value);
            // file name is arbitrary, file will be deleted after un-zipping
            File file = new File(MultiFileTools.getWorkingDirectory(), "scenario.zip");
            HttpDownloader.download(value, ContentType.APPLICATION_ZIP).to(file);

            Unzip.of(file, MultiFileTools.getWorkingDirectory(), true);

            file.delete();
        }
    }
}
