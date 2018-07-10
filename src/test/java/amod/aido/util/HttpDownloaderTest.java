/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.amodeus.util.math.UserHome;
import junit.framework.TestCase;

public class HttpDownloaderTest extends TestCase {
    public void testSimple() throws IOException {
        File file = UserHome.file("favicon.ico");
        assertFalse(file.exists());

        HttpDownloader.download("http://www.djtascha.de/favicon.ico").to(file);
        assertTrue(file.isFile());

        file.delete();
    }

    public void testHttps() throws IOException {
        File file = UserHome.file("scenario.zip");
        assertFalse(file.exists());

        HttpDownloader.download("https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWSGmy/download").to(file);

        assertTrue(file.isFile());
        assertEquals(file.length(), 5310145);

        file.delete();
    }
}
