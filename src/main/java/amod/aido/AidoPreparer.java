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
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.util.io.ProvideAVConfig;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.framework.AVConfigGroup;

/* package */ class AidoPreparer {

    private final Population population;
    private final ScenarioOptions scenOpt;
    private final Config configMatsim;
    private final Network network;

    /** loads scenario preparer in the {@link File} workingDirectory
     * 
     * @param workingDirectory
     * @throws MalformedURLException
     * @throws Exception */
    public AidoPreparer(File workingDirectory) throws MalformedURLException, Exception {
        Static.setup();

        /** amodeus options */
        scenOpt = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** MATSim config */
        AVConfigGroup avCg = new AVConfigGroup();
        configMatsim = ConfigUtils.loadConfig(scenOpt.getPreparerConfigName(), avCg);

        Scenario scenario = ScenarioUtils.loadScenario(configMatsim);

        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        this.network = NetworkPreparer.run(network, scenOpt);

        /** adaption of MATSim population, e.g., radius cutting */
        population = scenario.getPopulation();
    }

    public void run2(int numReqDes, int fleetSize) throws MalformedURLException, Exception {
        long apoSeed = 1234;
        AidoPopulationPreparer.run(network, population, scenOpt, configMatsim, apoSeed, numReqDes);

        /** creating a virtual network, e.g., for dispatchers using a graph structure on the city */
        VirtualNetworkPreparer.INSTANCE.create(network, population, scenOpt, fleetSize);

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(configMatsim, scenOpt);
    }

    public Tensor getBoundingBox() {
        /** send initial data (bounding box), {{minX, minY}, {maxX, maxY}} */
        double[] bbox = NetworkUtils.getBoundingBox(network.getNodes().values());

        return Tensors.of(
                TensorCoords.toTensor( //
                        scenOpt.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bbox[0], bbox[1]))), //
                TensorCoords.toTensor( //
                        scenOpt.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bbox[2], bbox[3]))));
    }

    public Population getPopulation() {
        return population;
    }

}
