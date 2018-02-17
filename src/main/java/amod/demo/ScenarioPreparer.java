package amod.demo;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.prep.FacilitiesPreparer;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.matsim.av.framework.AVConfigGroup;

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

    public static void run(File workingDirectory) throws MalformedURLException, Exception {

        // run preparer in simulation working directory
        ScenarioOptions scenarioOptions = ScenarioOptions.load(workingDirectory);

        // load Settings from IDSC Options
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // 1) cut network (and reduce population to new network)
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        // 2) adapt the population to new network
        Population population = scenario.getPopulation();
        PopulationPreparer.run(network, population, scenarioOptions, config);

        // 3) adapt the population to new network
        ActivityFacilities facilities = scenario.getActivityFacilities();
        FacilitiesPreparer.run(facilities, population, scenarioOptions);

        // 4) create virtual Network
        VirtualNetworkPreparer.run(network, population, scenarioOptions);

        // 5) save a simulation config file
        // IncludeActTypeOf.BaselineCH(config); // Only needed in Some Scenarios
        createSimulationConfigFile(config, scenarioOptions);

    }

    public static void createSimulationConfigFile(Config fullConfig, ScenarioOptions scenOptions) {

        // change population and network such that converted is loaded
        fullConfig.network().setInputFile(scenOptions.getPreparedNetworkName() + ".xml.gz");
        fullConfig.plans().setInputFile(scenOptions.getPreparedPopulationName() + ".xml.gz");
        fullConfig.facilities().setInputFile(scenOptions.getPreparedFacilitiesName() + ".xml.gz");

        // // Add activities which are not set. (SCenario dependant)
        // IncludeActTypeOf.baselineCH(fullConfig);

        AVConfigGroup avConfigGroup = new AVConfigGroup();
        fullConfig.addModule(avConfigGroup);

        // save under correct name
        new ConfigWriter(fullConfig).writeFileV2(scenOptions.getSimulationConfigName());
    }
}