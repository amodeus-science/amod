/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket;

import java.io.File;
import java.net.MalformedURLException;

import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.modal.GeneratorConfig;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import amodeus.amod.ext.Static;
import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.TensorCoords;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.prep.ConfigCreator;
import amodeus.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class SocketPreparer {
    private final Population population;
    private final ScenarioOptions scenOpt;
    private final Config config;
    private final Network network;
    private final MatsimAmodeusDatabase db;
    private final int numRt;

    /** loads scenario preparer in the {@link File} workingDirectory
     * 
     * @param workingDirectory
     * @throws MalformedURLException
     * @throws Exception */
    public SocketPreparer(File workingDirectory) throws MalformedURLException, Exception {
        Static.setup();

        /** amodeus options */
        scenOpt = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** MATSim config */
        // configMatsim = ConfigUtils.loadConfig(scenOpt.getPreparerConfigName());
        AmodeusConfigGroup avConfigGroup = new AmodeusConfigGroup();
        config = ConfigUtils.loadConfig(scenOpt.getPreparerConfigName(), avConfigGroup);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        GeneratorConfig genConfig = avConfigGroup.getModes().values().iterator().next().getGeneratorConfig();
        numRt = genConfig.getNumberOfVehicles();
        System.out.println("socketPrep NumberOfVehicles=" + numRt);

        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        this.network = NetworkPreparer.run(network, scenOpt);

        /** adaption of MATSim population, e.g., radius cutting */
        population = scenario.getPopulation();

        LocationSpec locationSpec = scenOpt.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();
        this.db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
    }

    /** second part of preparer
     * 
     * @param numReqDes
     * @throws Exception */
    public void run2(int numReqDes) throws Exception {
        long apoSeed = 1234;
        SocketPopulationPreparer.run(network, population, scenOpt, config, apoSeed, numReqDes);

        /** creating a virtual network, e.g., for dispatchers using a graph structure on the city */
        // int endTime = (int) config.qsim().getEndTime();
        // VirtualNetworkPreparer.INSTANCE.create(network, population, scenOpt, numRt,endTime);

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(config, scenOpt);
    }

    public Tensor getBoundingBox() {
        /** send initial data (bounding box), {{minX, minY}, {maxX, maxY}} */
        double[] bbox = NetworkUtils.getBoundingBox(network.getNodes().values());

        return Tensors.of(TensorCoords.toTensor( //
                scenOpt.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bbox[0], bbox[1]))), //
                TensorCoords.toTensor( //
                        scenOpt.getLocationSpec().referenceFrame().coords_toWGS84().transform(new Coord(bbox[2], bbox[3]))));
    }

    public Population getPopulation() {
        return population;
    }

    public MatsimAmodeusDatabase getDatabase() {
        return db;
    }
}
