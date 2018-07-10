/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import amod.aido.util.ContentType;
import amod.aido.util.HttpDownloader;
import amod.aido.util.PropertiesImport;
import ch.ethz.idsc.amodeus.util.math.UserHome;

/* package */ enum AidoScenarioDownload {

    ;

    /** @param key for instance "SanFrancisco"
     * @param file
     * @throws IOException */
    /* package */ static void download(String key, File file) throws IOException {

        Properties properties = PropertiesImport.properties("/aido/scenarios.properties");

        properties.list(System.out);

        if (properties.containsKey(key)) {

            String value = properties.getProperty(key);
            System.out.println(value);
            HttpDownloader.download(value, ContentType.APPLICATION_ZIP).to(file);
        }

        // TODO unzip and move into saveDir
    }

    public static void main(String[] args) throws Exception {
        try {
            download("SanFrancisco2", UserHome.file("aido-scenario.zip"));
        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }

    }

}
