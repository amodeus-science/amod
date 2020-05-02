package ch.ethz.idsc.amod;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.AfterClass;
import org.junit.Test;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.socket.core.SocketScenarioDownload;
import ch.ethz.idsc.tensor.io.DeleteDirectory;

public class DemoTest {

    @Test
    public void test() throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        // 1 download scenario
        try {
            SocketScenarioDownload.extract(workingDirectory, "SanFrancisco20200502");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2 run preparer
        ScenarioPreparer.main(null);
        // 3 run server
        ScenarioServer.main(null);
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        DeleteDirectory.of(new File(workingDirectory, "AmodeusOptions.properties"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "LPOptions.properties"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "config.xml"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "config_full.xml"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "linkSpeedData"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "preparedNetwork.xml"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "preparedPopulation.xml"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "preparedPopulation.xml.gz"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "preparedNetwork.xml.gz"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "population.xml.gz"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "network.xml.gz"), 0, 1);
        DeleteDirectory.of(new File(workingDirectory, "virtualNetwork/travelData"), 0,1);
        DeleteDirectory.of(new File(workingDirectory, "virtualNetwork/virtualNetwork"), 0,1);
        DeleteDirectory.of(new File(workingDirectory, "virtualNetwork"), 0,1);
        DeleteDirectory.of(new File(workingDirectory, "output"), 5, 15700);
        DeleteDirectory.of(new File(workingDirectory, "scenario.zip"), 0, 1);
    }

}
