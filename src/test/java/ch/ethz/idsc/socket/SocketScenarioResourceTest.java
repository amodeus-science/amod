/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket;

import java.io.File;
import java.io.IOException;
import java.util.List;

import amodeus.amodeus.util.io.MultiFileTools;
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
