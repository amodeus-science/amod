/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.aido.core;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import ch.ethz.idsc.amodeus.util.io.ContentType;
import ch.ethz.idsc.amodeus.util.io.Unzip;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.io.URLFetch;

public enum AidoScenarioDownload {
    ;

    /** @param key for instance "SanFrancisco.20080519"
     * @throws Exception */
    public static void extract(File workingDirecotry, String key) throws IOException {
        File file = new File(workingDirecotry, "scenario.zip");
        of(key, file);
        Unzip.of(file, workingDirecotry, true);
        file.delete();
    }

    /** @param key for instance "SanFrancisco.20080519"
     * @param file local target
     * @throws Exception */
    public static void of(String key, File file) throws IOException {
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        if (properties.containsKey(key)) {
            /** chosing scenario */
            String value = properties.getProperty(key);
            System.out.println("scenario: " + value);
            /** file name is arbitrary, file will be deleted after un-zipping */
            try (URLFetch urlFetch = new URLFetch(value)) {
                ContentType.APPLICATION_ZIP.require(urlFetch.contentType());
                urlFetch.download(file);
            }
            return;
        }
        throw new RuntimeException();
    }
}
