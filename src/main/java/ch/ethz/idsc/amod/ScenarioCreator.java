package ch.ethz.idsc.amod;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodtaxi.scenario.ScenarioCreation;
import ch.ethz.idsc.amodtaxi.scenario.chicago.ChicagoScenarioCreation;
import ch.ethz.idsc.amodtaxi.scenario.sanfrancisco.SanFranciscoScenarioCreation;

/** provides a quick access to the implementations of {@link ScenarioCreation}:
 * {@link ChicagoScenarioCreation}
 * {@link SanFranciscoScenarioCreation} */
public enum ScenarioCreator {
    CHICAGO {
        @Override
        public ScenarioCreation in(File workingDirectory) throws Exception {
            return ChicagoScenarioCreation.in(workingDirectory);
        }
    },
    SAN_FRANCISCO {
        @Override
        public ScenarioCreation in(File workingDirectory) throws Exception {
            return SanFranciscoScenarioCreation.of(workingDirectory, (File) null).get(0);
        }
    };

    /** creates a scenario with some basic settings in
     * @param workingDirectory
     * @return {@link ScenarioCreation}
     * @throws Exception if scenario could not be properly created */
    public abstract ScenarioCreation in(File workingDirectory) throws Exception;

    // ---

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            throw new RuntimeException("No scenario specified! Provide scenario name as argument.");

        Map<String,ScenarioCreator> scenarios = new HashMap<>();
        scenarios.put("chicago", ScenarioCreator.CHICAGO);
        scenarios.put("sanfrancisco", ScenarioCreator.SAN_FRANCISCO);
        scenarios.put("san_francisco", ScenarioCreator.SAN_FRANCISCO);
        scenarios.put("san-francisco", ScenarioCreator.SAN_FRANCISCO);

        ScenarioCreation scenario = scenarios.get(args[0].toLowerCase()).in(MultiFileTools.getDefaultWorkingDirectory());
        GlobalAssert.that(scenario.directory().exists());
        System.out.println("Created a ready to use AMoDeus scenario in " + scenario.directory());
    }
}
