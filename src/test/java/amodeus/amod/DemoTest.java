package amodeus.amod;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Test;

import amodeus.amodeus.util.io.Locate;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.io.Unzip;
import amodeus.amodtaxi.scenario.ScenarioCreation;
import amodeus.amodtaxi.scenario.sanfrancisco.TraceFileChoice;
import ch.ethz.idsc.tensor.ext.DeleteDirectory;

public class DemoTest {

    @Test
    public void test() throws Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();

        try {
            File map = new File(Locate.repoFolder(ScenarioCreator.class, "amod"), "src/main/resources/scenario/SanFrancisco/network_pt2matsim.zip");
            Unzip.of(map, workingDirectory, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 1 download scenario
        ScenarioCreation creation = ScenarioCreator.SAN_FRANCISCO.in(workingDirectory);
        // 2 run preparer
        ScenarioPreparer.run(creation.directory());
        // 3 run server
        ScenarioServer.simulate(creation.directory());
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        new File(workingDirectory, "AmodeusOptions.properties").delete();
        new File(workingDirectory, "LPOptions.properties").delete();
        new File(workingDirectory, "config_full.xml").delete();
        new File(workingDirectory, "map.osm").delete();
        new File(workingDirectory, "network.xml").delete();
        new File(workingDirectory, "network.xml.gz").delete();
        new File(workingDirectory, "network_pt2matsim.xml").delete();
        new File(workingDirectory, "pt2matsim_settings.xml").delete();

        if (TraceFileChoice.DEFAULT_DATA.exists())
            DeleteDirectory.of(TraceFileChoice.DEFAULT_DATA, 1, 5);

        DeleteDirectory.of(new File(workingDirectory, "Scenario"), 2, 14);
        DeleteDirectory.of(new File(workingDirectory, "2008-06-04"), 2, 14);
        DeleteDirectory.of(new File(workingDirectory, "output"), 5, 11000);
    }
}
