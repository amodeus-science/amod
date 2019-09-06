/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.io.DeleteDirectory;

/* package */ enum InitialFiles {
    ;

    public static void copyToDir(File procDir, File dataDir) throws Exception {

        System.out.println("Copying data files from : " + dataDir);
        System.out.println("to:                       " + procDir);

        /** empty the processing folder */
        System.err.println(procDir.getAbsolutePath());
        if (procDir.exists()) {
            System.err.println("WARN All files in the that folder will be deleted in:");
            for (int i = 3; i > 0; i--) {
                Thread.sleep(1000);
                System.err.println(i + " seconds");
            }
            DeleteDirectory.of(procDir, 2, 14);
            procDir.mkdir();
        } else
            GlobalAssert.that(procDir.mkdir());

        /** copy initial config files */
        CopyOption[] options = new CopyOption[] { //
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };

        String[] fileNames = new String[] { //
                "AmodeusOptions.properties", "av.xml", "config_full.xml", "network.xml", "network.xml.gz" };

        for (String fileName : fileNames) {
            File sourceFile = new File(dataDir, fileName);
            if (sourceFile.exists()) {
                Path source = Paths.get(sourceFile.getAbsolutePath());
                Path target = Paths.get(procDir.getPath(), fileName);
                Files.copy(source, target, options);
            } else
                new IOException(sourceFile.getAbsolutePath() + " does not exist!").printStackTrace();
        }
    }
}
