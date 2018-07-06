/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.prep.ConfigCreator;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

enum AidoPreparer {
    ;

    /** loads scenario preparer in the {@link File} workingDirectory
     * 
     * @param workingDirectory
     * @throws MalformedURLException
     * @throws Exception */
    public static Tensor run(File workingDirectory, double populRed) throws MalformedURLException, Exception {
        Static.setup();

        /** amodeus options */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** MATSim config */
        Config configMatsim = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName());
        Scenario scenario = ScenarioUtils.loadScenario(configMatsim);

        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        /** adaption of MATSim population, e.g., radius cutting */
        Population population = scenario.getPopulation();
        scenarioOptions.setMaxPopulationSize((int) (population.getPersons().size() * populRed));
        long apoSeed = 1234;
        PopulationPreparer.run(network, population, scenarioOptions, configMatsim, apoSeed);

        // /** creating a virtual network, e.g., for dispatchers using a graph structure on the city
        // */
        // VirtualNetworkPreparer.run(network, population, scenarioOptions);

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(configMatsim, scenarioOptions);

        /** send initial data (bounding box) */
        double[] bounding = NetworkUtils.getBoundingBox(network.getNodes().values()); // {minX,
                                                                                      // minY, maxX,
                                                                                      // maxY}
        Tensor initialInfo = Tensors.empty();
        initialInfo.append(TensorCoords.toTensor(scenarioOptions.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bounding[0], bounding[1]))));

        initialInfo.append(TensorCoords.toTensor(scenarioOptions.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bounding[2], bounding[3]))));

        return initialInfo;
    }

}
