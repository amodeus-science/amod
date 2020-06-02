/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Objects;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.generator.RandomDensityGenerator;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.linkspeed.LinkSpeedUtils;
import amodeus.amodeus.linkspeed.TaxiTravelTimeRouter;
import amodeus.amodeus.linkspeed.TrafficDataModule;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationServer;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.AddCoordinatesToActivities;
import amodeus.amodeus.util.net.StringSocket;
import org.matsim.amodeus.AmodeusConfigurator;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.framework.AmodeusUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import amodeus.amod.ext.Static;
import amodeus.socket.core.SocketDispatcherHost;

/** only one ScenarioServer can run at one time, since a fixed network port is
 * reserved to serve the simulation status */
/* package */ class SocketServer {

    private File configFile;
    private File outputDirectory;
    private Network network;
    private ReferenceFrame referenceFrame;
    private ScenarioOptions scenarioOptions;

    /** runs a simulation run using input data from Amodeus.properties, av.xml and MATSim config.xml
     * 
     * @throws MalformedURLException
     * @throws Exception */

    public void simulate(StringSocket stringSocket, int numReqTot, //
            File workingDirectory) throws MalformedURLException, Exception {
        Static.setup();
        /** working directory and options */
        scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client, for fals the ScenarioServer starts the simulation
         * immediately */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        configFile = new File(scenarioOptions.getSimulationConfigName());
        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        referenceFrame = locationSpec.referenceFrame();

        /** open server port for clients to connect to */
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        /** load MATSim configs - including av.xml configurations, load routing packages */
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AmodeusConfigGroup(), dvrpConfigGroup);
        config.planCalcScore().addActivityParams(new ActivityParams("activity"));
        // TODO @Sebastian fix this to meaningful values, remove, or add comment
        // this was added because there are sometimes problems, is there a more elegant option?
        for (ActivityParams activityParams : config.planCalcScore().getActivityParams()) {
            activityParams.setTypicalDuration(3600.0);
        }

        /** load MATSim scenario for simulation */
        Scenario scenario = ScenarioUtils.loadScenario(config);
        AddCoordinatesToActivities.run(scenario);
        network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(Objects.nonNull(network));
        GlobalAssert.that(Objects.nonNull(population));

        Objects.requireNonNull(network);
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        Controler controller = new Controler(scenario);
        AmodeusConfigurator.configureController(controller, db, scenarioOptions);

        /** try to load link speed data and use for speed adaption in network */
        try {
            File linkSpeedDataFile = new File(scenarioOptions.getLinkSpeedDataName());
            System.out.println(linkSpeedDataFile.toString());
            LinkSpeedDataContainer lsData = LinkSpeedUtils.loadLinkSpeedData(linkSpeedDataFile);
            controller.addOverridingQSimModule(new TrafficDataModule(lsData));
        } catch (Exception exception) {
            System.err.println("Unable to load linkspeed data, freeflow speeds will be used in the simulation.");
            exception.printStackTrace();
        }

        controller.addOverridingModule(new SocketModule(stringSocket, numReqTot));

        /** Custom router that ensures same network speeds as taxis in original data set. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TaxiTravelTimeRouter.Factory.class);
                AmodeusUtils.bindRouterFactory(binder(), TaxiTravelTimeRouter.class.getSimpleName()).to(TaxiTravelTimeRouter.Factory.class);
            }
        });

        /** adding the dispatcher to receive and process string fleet commands */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), "SocketDispatcherHost", SocketDispatcherHost.Factory.class);
            }
        });

        /** adding an initial vehicle placer */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.bindGeneratorFactory(binder(), RandomDensityGenerator.class.getSimpleName()).//
                to(RandomDensityGenerator.Factory.class);
            }
        });

        /** run simulation */
        controller.run();

        /** close port for visualizaiton */
        SimulationServer.INSTANCE.stopAccepting();

        /** perform analysis of simulation */
        /** output directory for saving results */
        outputDirectory = new File(config.controler().getOutputDirectory());

    }

    /* package */ File getOutputDirectory() {
        return outputDirectory;
    }

    /* package */ File getConfigFile() {
        return configFile;
    }

    /* package */ Network getNetwork() {
        return network;
    }

    /* package */ ReferenceFrame getReferenceFrame() {
        return referenceFrame;
    }

    /* package */ ScenarioOptions getScenarioOptions() {
        return scenarioOptions;
    }
}
