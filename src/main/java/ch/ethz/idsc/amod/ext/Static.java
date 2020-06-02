/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.LocationSpecDatabase;
import amodeus.amodeus.options.LPOptionsBase;
import org.gnu.glpk.GLPK;

public enum Static {
    ;

    public static void setup() {
        for (LocationSpec locationSpec : UserLocationSpecs.values())
            LocationSpecDatabase.INSTANCE.put(locationSpec);
    }

    public static void checkGLPKLib() {
        try {
            System.out.println("Working with GLPK version " + GLPK.glp_version());
        } catch (Exception exception) {
            System.err.println(glpInfo());
        }
    }

    public static String glpInfo() {
        return "GLPK for java is necessary for some configurations of the preparer or server. \n " + "In order to install it, follow the instructions provided at\n: "
                + "http://glpk-java.sourceforge.net/gettingStarted.html \n" + "In order to work properly, either the location of the GLPK library must be  specified in \n"
                + "the environment variable, using for instance the command" + "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib/jni \n"
                + "where /usr/local/lib/jni  is the path where the file libglpk_java.so is located \n"
                + "in your installation. Alternatively, the path can also be supplied as a JAVA runtime \n" + "argument, e.g., -Djava.library.path=/usr/local/lib/jni";
    }

    /** For many dispatchers, a linear program is solved previous to the simulation start
     * to initialize data structures for rebalancing. This requires the GLPK library to work.
     * This call ensures that no linear program must be solved by changing the file LPOptions.properties
     * in the @param workingDirectory to ensure that initial users do not get stopped by this obstacle. */
    public static void setLPtoNone(File workingDirectory) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        File file = new File(workingDirectory, LPOptionsBase.OPTIONSFILENAME);
        try (InputStream inputStream = new FileInputStream(file)) {
            props.load(inputStream);
        }
        try (OutputStream outputStream = new FileOutputStream(file)) {
            props.setProperty("LPSolver", "NONE");
            props.store(outputStream, null);
        }
    }
}
