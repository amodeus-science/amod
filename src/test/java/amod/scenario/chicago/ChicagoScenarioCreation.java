package amod.scenario.chicago;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import junit.framework.Assert;

public class ChicagoScenarioCreation {

    public void test() throws IOException, Exception {
        File workingDir = MultiFileTools.getDefaultWorkingDirectory();
        StaticHelper.setupTest(workingDir);
        CreateChicagoScenario.run(workingDir);
        // TODO add some tests, e.g., running the scenario
        Assert.assertTrue(true);
        StaticHelper.cleanUpTest(workingDir);
    }
}
