/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Objects;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

import amod.demo.analysis.CustomAnalysis;
import amod.demo.dispatcher.DemoDispatcher;
import amod.demo.ext.Static;
import amod.demo.generator.DemoGenerator;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedUtils;
import ch.ethz.idsc.amodeus.linkspeed.TrafficDataModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDatabaseModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDispatcherModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleToVSGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVirtualNetworkModule;
import ch.ethz.idsc.amodeus.net.DatabaseModule;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.SimulationServer;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.framework.AVConfigGroup;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.framework.AVUtils;

/** only one ScenarioServer can run at one time, since a fixed network port is
 * reserved to serve the simulation status */
public enum ScenarioServer {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        simulate();
        // General todo's to be completed:
        // TODO add time-varying dispatcher

    }

    /** runs a simulation run using input data from Amodeus.properties, av.xml and MATSim config.xml
     * 
     * @throws MalformedURLException
     * @throws Exception */
    public static void simulate() throws MalformedURLException, Exception {
        Static.setup();

        Static.checkGLPKLib();

        /** working directory and options */
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client, for fals the ScenarioServer starts the simulation
         * immediately */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        File configFile = new File(workingDirectory, scenarioOptions.getSimulationConfigName());
        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** open server port for clients to connect to */
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        /** load MATSim configs - including av.xml configurations, load routing packages */
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        config.planCalcScore().addActivityParams(new ActivityParams("activity"));
        for (ActivityParams activityParams : config.planCalcScore().getActivityParams()) {
            activityParams.setTypicalDuration(3600.0); // TODO fix this to meaningful values
        }

        /** output directory for saving results */
        String outputdirectory = config.controler().getOutputDirectory();

        /** load MATSim scenario for simulation */
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(Objects.nonNull(network));
        GlobalAssert.that(Objects.nonNull(population));

        // load linkSpeedData
        File linkSpeedDataFile = new File(workingDirectory, scenarioOptions.getLinkSpeedDataName());
        System.out.println(linkSpeedDataFile.toString());
        LinkSpeedDataContainer lsData = LinkSpeedUtils.loadLinkSpeedData(linkSpeedDataFile);

        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new DvrpTravelTimeModule());
        controler.addOverridingModule(new TrafficDataModule(lsData));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule());
        controler.addOverridingModule(new AmodeusVehicleGeneratorModule());
        controler.addOverridingModule(new AmodeusDispatcherModule());
        controler.addOverridingModule(new AmodeusDatabaseModule(db));

        /** uncomment to include custom routers
         * controler.addOverridingModule(new AbstractModule() {
         * 
         * @Override
         *           public void install() {
         *           bind(CustomRouter.Factory.class);
         *           AVUtils.bindRouterFactory(binder(),
         *           CustomRouter.class.getSimpleName()).to(CustomRouter.Factory.class);
         * 
         *           }
         *           }); */

        /** You need to activate this if you want to use a dispatcher that needs a virtual
         * network! */
        controler.addOverridingModule(new AmodeusVirtualNetworkModule());
        controler.addOverridingModule(new AmodeusVehicleToVSGeneratorModule());

        // ===============================================

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(Key.get(Network.class, Names.named("dvrp_routing"))).to(Network.class);
            }
        });
        controler.addOverridingModule(new AmodeusModule());

        /** here an additional user-defined dispatcher is added, functionality in class
         * DemoDispatcher */
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AVUtils.registerDispatcherFactory(binder(), "DemoDispatcher", DemoDispatcher.Factory.class);
            }
        });
        // TODO @ Lukas this produces problems.
        // controler.addOverridingModule(new AbstractModule() {
        // @Override
        // public void install() {
        // AVUtils.registerDispatcherFactory(binder(), "DemoDispatcherShared",
        // DemoDispatcherShared.Factory.class);
        // }
        // });

        /** here an additional user-defined initial placement logic called generator is added,
         * functionality in class DemoGenerator */
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AVUtils.registerGeneratorFactory(binder(), "DemoGenerator", DemoGenerator.Factory.class);
            }
        });

        /** run simulation */
        controler.run();

        /** close port for visualizaiton */
        SimulationServer.INSTANCE.stopAccepting();

        /** perform analysis of simulation, a demo of how to add custom
         * analysis methods is provided in the package amod.demo.analysis */
        Analysis analysis = Analysis.setup(null, configFile, new File(outputdirectory), db);
        CustomAnalysis.addTo(analysis);
        analysis.run();

    }

    public static void clearMemory() {
        // TODO clear memory for the sequential server such that RAM is not limiting
    }
}