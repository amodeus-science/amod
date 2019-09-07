/* amod - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;

public class ChicagoScenarioCreation {

    public void test() throws IOException, Exception {
        File workingDir = MultiFileTools.getDefaultWorkingDirectory();
        StaticHelper.setupTest(workingDir);
        CreateChicagoScenario.run(workingDir);
        // TODO add some tests, e.g., running the scenario

        StaticHelper.cleanUpTest(workingDir);
    }
}
