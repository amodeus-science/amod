// code by jph
package amod.aido;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

public class AidoScenarioResourceTest extends TestCase {
    public void testLocal() throws IOException {
        List<File> list = AidoScenarioResource.extract("SanFrancisco.20080518");
        assertEquals(list.size(), 7);
        list.stream().forEach(File::delete);
    }

    public void testDownload() throws IOException {
        List<File> list = AidoScenarioResource.extract("SanFrancisco.20080519");
        assertEquals(list.size(), 7);
        list.stream().forEach(File::delete);
    }
}
