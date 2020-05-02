// code by jph
package ch.ethz.idsc.socket;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.socket.SocketScenarioResource;
import junit.framework.TestCase;

public class SocketScenarioResourceTest extends TestCase {
    public void testLocal() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = SocketScenarioResource.extract("SanFrancisco", workingDirectory);
        try {
            assertEquals(list.size(), 7);
        } finally {
            list.forEach(File::delete);
        }
    }

    public void testDownload() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        List<File> list = SocketScenarioResource.extract("SanFrancisco", workingDirectory);
        try {
            assertEquals(list.size(), 7);
        } finally {
            list.forEach(File::delete);
        }
    }
}
