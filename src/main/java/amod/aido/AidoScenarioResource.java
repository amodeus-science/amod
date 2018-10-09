/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import amod.aido.core.AidoScenarioDownload;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.io.Unzip;

public enum AidoScenarioResource {
    ;
    private static final String SCENARIO_ZIP = "scenario.zip";

    /** @param key for instance "SanFrancisco.20080518"
     * @throws IOException */
    public static List<File> extract(final String key) throws IOException {
        /** file name is arbitrary, file will be deleted after un-zipping */
        final File file = new File(MultiFileTools.getWorkingDirectory(), SCENARIO_ZIP);
        String resource = "/scenario/" + key.replace('.', '/') + "/" + SCENARIO_ZIP;
        try (InputStream inputStream = AidoScenarioResource.class.getResourceAsStream(resource)) {
            System.out.println("obtain as resource: [" + resource + "]");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while (0 < (length = inputStream.read(buffer)))
                    outputStream.write(buffer, 0, length);
            }

        } catch (Exception exception) {
            System.out.println("scenario not fount as resource: [" + resource + "]");
            AidoScenarioDownload.of(key, file);
        }
        List<File> list = Unzip.of(file, MultiFileTools.getWorkingDirectory(), true);
        file.delete();
        return list;
    }

}
