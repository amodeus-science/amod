// code by jph
package ch.ethz.idsc.aido;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import junit.framework.TestCase;

public class AidoScenarioResourceTest extends TestCase {
    public void testLocal() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = AidoScenarioResource.extract("SanFrancisco", workingDirectory);
        try {
            assertEquals(list.size(), 7);
        } finally {
            list.forEach(File::delete);
        }
    }

    public void testDownload() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = AidoScenarioResource.extract("SanFrancisco", workingDirectory);
        try {
            assertEquals(list.size(), 7);
        } finally {
            list.forEach(File::delete);
        }
    }
}
