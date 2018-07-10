/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import ch.ethz.idsc.tensor.io.ResourceData;

// TODO JAN obsolete with tensor 057  
public enum PropertiesImport {
    ;
    /** load from resources
     * 
     * @param string
     * @return properties loaded from resource */
    public static Properties properties(String string) {
        try (InputStream inputStream = ResourceData.class.getResourceAsStream(string)) {
            return properties(inputStream);
        } catch (Exception exception) {
            // ---
        }
        return null;
    }

    /** @param file
     * @return properties loaded from given file
     * @throws FileNotFoundException
     * @throws IOException */
    public static Properties properties(File file) throws FileNotFoundException, IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return properties(inputStream);
        }
    }

    private static Properties properties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
