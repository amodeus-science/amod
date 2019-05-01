/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
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
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.prep.ConfigCreator;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.io.ProvideAVConfig;
import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.framework.AVConfigGroup;

/** Class to prepare a given scenario for MATSim, includes preparation of
 * network, population, creation of virtualNetwork and travelData objects. As an example
 * a user may want to restrict the population size to few 100s of agents to run simulations
 * quickly during testing, or the network should be reduced to a certain area. */
public enum ScenarioPreparer {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        run(workingDirectory);
    }

    /** loads scenario preparer in the {@link File} workingDirectory @param workingDirectory
     * 
     * @throws MalformedURLException
     * @throws Exception */
    public static void run(File workingDirectory) throws MalformedURLException, Exception {
        Static.setup();
        Static.checkGLPKLib();

        /** The {@link ScenarioOptions} contain amodeus specific options. Currently there are 3
         * options files:
         * - MATSim configurations (config.xml)
         * - AV package configurations (av.xml)
         * - AMoDeus configurations (AmodeusOptions.properties).
         * 
         * The number of configs is planned to be reduced in subsequent refactoring steps. */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** MATSim config */
        AVConfigGroup avConfigGroup = new AVConfigGroup();
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName(), avConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        AVConfig avConfig = ProvideAVConfig.with(config, avConfigGroup);
        AVGeneratorConfig genConfig = avConfig.getOperatorConfigs().iterator().next().getGeneratorConfig();
        int numRt = (int) genConfig.getNumberOfVehicles();
        System.out.println("NumberOfVehicles=" + numRt);

        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        /** adaption of MATSim population, e.g., radius cutting */
        Population population = scenario.getPopulation();
        long apoSeed = 1234;
        PopulationPreparer.run(network, population, scenarioOptions, config, apoSeed);

        /** creating a virtual network, e.g., for operational policies requiring a graph structure on the city */
        int endTime = (int) config.qsim().getEndTime();
        VirtualNetworkPreparer.INSTANCE.create(network, population, scenarioOptions, numRt, endTime); //

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(config, scenarioOptions);
    }
}