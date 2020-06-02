/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.io.File;
import java.io.IOException;

import amodeus.amodeus.util.io.ContentType;
import ch.ethz.idsc.tensor.io.HomeDirectory;
import ch.ethz.idsc.tensor.io.URLFetch;
import junit.framework.TestCase;

public class HttpDownloaderTest extends TestCase {
    public void testSimple() throws IOException {
        File file = HomeDirectory.file("favicon.ico");
        assertFalse(file.exists());

        try (URLFetch urlFetch = new URLFetch("http://www.djtascha.de/favicon.ico")) {
            ContentType.IMAGE_XICON.require(urlFetch.contentType());
            urlFetch.download(file);
        }

        try {
            assertTrue(file.isFile());
        } finally {
            file.delete();
        }
    }

    public void testHttps() throws IOException {
        File file = HomeDirectory.file("scenario.zip");
        System.out.println(file.getAbsolutePath());
        assertFalse(file.exists());

        try (URLFetch urlFetch = new URLFetch("https://polybox.ethz.ch/index.php/s/o5lsGffyRsspkJP/download")) {
            ContentType.APPLICATION_ZIP.require(urlFetch.contentType());
            urlFetch.download(file);
        }

        try {
            assertTrue(file.isFile());
        } finally {
            file.delete();
        }
    }

    public void testFail() {
        File file = HomeDirectory.file("scenario-does-not-exist.zip");
        assertFalse(file.exists());
        try {
            try (URLFetch urlFetch = new URLFetch( //
                    "https://polybox.ethz.ch/index.php/s/C3QUuk3cuWWS1Gmy/download123")) { //
                ContentType.APPLICATION_ZIP.require(urlFetch.contentType());
            }
            fail();
        } catch (Exception exception) {
            // ---
        }
        assertFalse(file.exists());
    }
}
