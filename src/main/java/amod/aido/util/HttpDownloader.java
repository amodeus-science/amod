/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpDownloader {
    private static final int BUFFER_SIZE = 4096;

    public static HttpDownloader download(String fileURL) {
        return new HttpDownloader(fileURL);
    }

    // ---
    private final String fileURL;

    private HttpDownloader(String fileURL) {
        this.fileURL = fileURL;
    }

    public void to(File file) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        int responseCode = httpURLConnection.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // tests show that often: disposition == null
            String disposition = httpURLConnection.getHeaderField("Content-Disposition");
            String contentType = httpURLConnection.getContentType();
            int contentLength = httpURLConnection.getContentLength();

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);

            byte[] buffer = new byte[BUFFER_SIZE];
            // opens input stream from the HTTP connection
            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                // opens an output stream to save into file
                try (OutputStream outputStream = new FileOutputStream(file)) {
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, bytesRead);

                }
            }

            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpURLConnection.disconnect();
    }
}