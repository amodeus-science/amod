// code by jph
package amod.aido;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import junit.framework.TestCase;

public class AidoScenarioResourceTest extends TestCase {
    public void testLocal() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = AidoScenarioResource.extract("SanFrancisco.20080518", workingDirectory);
        assertEquals(list.size(), 7);
        list.stream().forEach(File::delete);
    }

    public void testDownload() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = AidoScenarioResource.extract("SanFrancisco.20080519", workingDirectory);
        assertEquals(list.size(), 7);
        list.stream().forEach(File::delete);
    }
}
