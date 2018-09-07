/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import ch.ethz.idsc.amodeus.aido.util.ContentType;
import ch.ethz.idsc.amodeus.aido.util.HttpDownloader;
import ch.ethz.idsc.amodeus.aido.util.Unzip;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.io.ResourceData;

public enum ScenarioResourceData {
    ;
    private static final String SCENARIO_ZIP = "scenario.zip";

    /** @param key for instance "SanFrancisco"
     * @throws IOException */
    public static List<File> extract(final String key) throws IOException {
        /** file name is arbitrary, file will be deleted after un-zipping */
        final File file = new File(MultiFileTools.getWorkingDirectory(), SCENARIO_ZIP);
        String resource = "/scenario/" + key.replace('.', '/') + "/" + SCENARIO_ZIP;
        try (InputStream inputStream = ScenarioResourceData.class.getResourceAsStream(resource)) {
            System.out.println("obtain");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while (0 < (length = inputStream.read(buffer)))
                    outputStream.write(buffer, 0, length);
            }

        } catch (Exception exception) {
            System.out.println("scenario not fount as resource: [" + resource + "]");
        }
        // TODO use amodeus solution here
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        if (properties.containsKey(key)) {
            /** chosing scenario */
            String value = properties.getProperty(key);
            System.out.println("scenario: " + value);
            HttpDownloader.download(value, ContentType.APPLICATION_ZIP).to(file);
        }
        List<File> list = Unzip.of(file, MultiFileTools.getWorkingDirectory(), true);
        file.delete();
        return list;
    }

}
