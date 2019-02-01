/* amod - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.amodeus.prep.ConfigCreator;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.simulation.PreparerProperties;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;

/** Class to prepare a given scenario for MATSim, includes preparation of network, population, creation of virtualNetwork
 * and travelData objects. */
public enum ScenarioPreparerBasic {
    ;

    public static final long APOCALYPSE_SEED = 1234;

    public static void main(String[] args) throws MalformedURLException, Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory, APOCALYPSE_SEED);
    }

    public static void run(File workingDirectory, long apoSeed) throws MalformedURLException, Exception {

        /** Load all the required Sizes such as the Configs and the Scenario */
        PreparerProperties prepProp = new PreparerProperties(workingDirectory);

        /** adaption of MATSim network, e.g., radius cutting */
        Network networkFull = prepProp.getNetwork();
        Network network = NetworkPreparer.run(networkFull, prepProp.getScenarioOptions());

        /** adaption of MATSim population, e.g., radius cutting */
        Population population = prepProp.getPopulation();
        PopulationPreparer.run(network, population, prepProp.getScenarioOptions(), prepProp.getConfig(), apoSeed);

        /** creating a virtual network, e.g., for dispatchers using a graph structure on the city */
        VirtualNetworkPreparer.INSTANCE.create(network, population, prepProp.getScenarioOptions(), prepProp.getNumberRoboTaxis(), prepProp.getEndTime());

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(prepProp.getConfig(), prepProp.getScenarioOptions());

    }

}