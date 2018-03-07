package amod.demo;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Objects;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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

import amod.dispatcher.DemoDispatcher;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.html.DataCollector;
import ch.ethz.idsc.amodeus.html.Report;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusModule;
import ch.ethz.idsc.amodeus.matsim.mod.IDSCDispatcherModule;
import ch.ethz.idsc.amodeus.matsim.mod.IDSCGeneratorModule;
import ch.ethz.idsc.amodeus.net.DatabaseModule;
import ch.ethz.idsc.amodeus.net.MatsimStaticDatabase;
import ch.ethz.idsc.amodeus.net.SimulationServer;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.traveldata.TravelData;
import ch.ethz.idsc.amodeus.traveldata.TravelDataGet;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetworkGet;
import ch.ethz.matsim.av.framework.AVConfigGroup;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.framework.AVUtils;

/** only one ScenarioServer can run at one time, since a fixed network port is
 * reserved to serve the simulation status */
public enum ScenarioServer {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        simulate();
    }

    /* package */ public static void simulate() throws MalformedURLException, Exception {
        // load options
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions scenarioOptions = ScenarioOptions.load(workingDirectory);
        System.out.println("Start--------------------"); // added no

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        File configFile = new File(workingDirectory, scenarioOptions.getSimulationConfigName());
        // Locationspec needs to be set manually in IDSCOptions.properties
        // Referenceframe needs to be set manually in IDSCOptions.properties
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        // open server port for clients to connect to
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        // load MATSim configs - including av.xml where dispatcher is selected.
        System.out.println("loading config file " + configFile.getAbsoluteFile());

        GlobalAssert.that(configFile.exists()); // Test whether the config file
                                                // directory exists

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        config.planCalcScore().addActivityParams(new ActivityParams("activity"));
        for (ActivityParams activityParams : config.planCalcScore().getActivityParams()) {
            activityParams.setTypicalDuration(3600.0); // TODO fix this to meaningful values
        }

        // IncludeActTypeOf.zurichConsensus(config);
        // IncludeActTypeOf.artificial(config);

        String outputdirectory = config.controler().getOutputDirectory();
        System.out.println("outputdirectory = " + outputdirectory);

        // load scenario for simulation
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(network != null && population != null);

        // load linkSpeedData
        // File linkSpeedDataFile = new File(workingDirectory,
        // scenarioOptions.getLinkSpeedDataName());
        // System.out.println(linkSpeedDataFile.toString());
        // LinkSpeedDataContainer lsData =
        // LinkSpeedUtils.loadLinkSpeedData(linkSpeedDataFile);

        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        Controler controler = new Controler(scenario);

        // controler.addControlerListener(new ControlerDebugger(controler));
        // controler.addOverridingModule(new TrafficDataModule(lsData));
        controler.addOverridingModule(new DvrpTravelTimeModule());
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new DatabaseModule());
        // controler.addOverridingModule(new AVTravelTimeModule());
        controler.addOverridingModule(new IDSCGeneratorModule());
        controler.addOverridingModule(new IDSCDispatcherModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(Key.get(Network.class, Names.named("dvrp_routing"))).to(Network.class);
            }
        });
        controler.addOverridingModule(new AmodeusModule());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AVUtils.registerDispatcherFactory(binder(), "DemoDispatcher", DemoDispatcher.Factory.class);
            }
        });

        // run simulation
        controler.run();

        // close port for visualization
        SimulationServer.INSTANCE.stopAccepting();

        // perform analysis of results
        // try {
        // // FIXME this should never crash
        // new TrainXTravelTime(population).plotHistogram(workingDirectory);
        // } catch (Exception exception) {
        // System.err.println("something went wrong");
        // exception.printStackTrace();
        // }

        AnalysisSummary analyzeSummary = Analysis.now(configFile, outputdirectory, population, referenceFrame);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        //
        // MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        // Performa nceFleetSizeCalculator performanceFleetSizeCalculator = null;
        TravelData travelData = null;
        if (virtualNetwork != null) {
            // minimumFleetSizeCalculator = MinimumFleetSizeGet.readDefault();
            // performanceFleetSizeCalculator =
            // PerformanceFleetSizeGet.readDefault();
            // if (performanceFleetSizeCalculator != null) {
            // String dataFolderName = outputdirectory + "/data";
            // File relativeDirectory = new File(dataFolderName);
            // performanceFleetSizeCalculator.saveAndPlot(dataFolderName,
            // relativeDirectory);
            // }

            travelData = TravelDataGet.readDefault(virtualNetwork);
        }
        GlobalAssert.that(!Objects.isNull(travelData));

        new DataCollector(configFile, outputdirectory, analyzeSummary);

        // generate report
        Report.using(configFile, outputdirectory).generate();

    }

    public static void clearMemory() {
        // ToDO Clear the memory for the sequential Server such that the RAM is
        // not limited.

    }
}