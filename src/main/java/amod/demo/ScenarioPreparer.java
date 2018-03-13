package amod.demo;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.prep.ConfigCreator;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;

/** Class to prepare a given scenario for MATSim, includes preparation of
 * network, population, creation of virtualNetwork and travelData objects.
 * 
 * @author clruch */
public enum ScenarioPreparer {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    /** loads scenario preparer in the {@link File} workingDirectory
     * 
     * @param workingDirectory
     * @throws MalformedURLException
     * @throws Exception */
    public static void run(File workingDirectory) throws MalformedURLException, Exception {
        Static.setup();

        /** amodeus options */
        ScenarioOptions scenarioOptions = ScenarioOptions.load(workingDirectory);

        /** MATSim config */
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        /** adaption of MATSim population, e.g., radius cutting */
        Population population = scenario.getPopulation();
        PopulationPreparer.run(network, population, scenarioOptions, config);

        /** creating a virtual network, e.g., for dispatchers using a graph structure on the city */
        VirtualNetworkPreparer.run(network, population, scenarioOptions);

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(config, scenarioOptions);

    }

}