/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket.core;

import java.io.File;
import java.io.IOException;

import amodeus.amodeus.util.io.MultiFileTools;
import junit.framework.TestCase;

public class SocketScenarioDownloadTest extends TestCase {
    public void testSimple() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        File file = new File(workingDirectory, "scenario.zip"); // <3MB
        assertFalse(file.exists());

        boolean inactive = false;
        try {
            SocketScenarioDownload.of("SanFrancisco", file);
        } catch (IOException e) {
            if (e.getMessage().equals("503")) {
                System.err.println("currently no active competition");
                inactive = true;
            } else throw e;
        }

        try {
            assertTrue(inactive || file.isFile());
        } finally {
            file.delete();
        }
    }
}
