/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.socket.core.SocketScenarioDownload;
import junit.framework.TestCase;

public class AidoScenarioDownloadTest extends TestCase {
    public void testSimple() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        File file = new File(workingDirectory, "scenario.zip"); // <3MB
        assertFalse(file.exists());
        SocketScenarioDownload.of("SanFrancisco", file);
        assertTrue(file.isFile());
        file.delete();
    }
}
