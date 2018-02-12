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
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

import amod.dispatcher.DemoDispatcher;
import ch.ethz.idsc.amodeustools.analysis.AnalyzeAll;
import ch.ethz.idsc.amodeustools.analysis.AnalyzeSummary;
import ch.ethz.idsc.amodeustools.data.ReferenceFrame;
import ch.ethz.idsc.amodeustools.filehandling.MultiFileTools;
import ch.ethz.idsc.amodeustools.html.DataCollector;
import ch.ethz.idsc.amodeustools.html.ReportGenerator;
import ch.ethz.idsc.amodeustools.matsim_decoupling.IDSCDispatcherModule;
import ch.ethz.idsc.amodeustools.matsim_decoupling.IDSCGeneratorModule;
import ch.ethz.idsc.amodeustools.matsim_decoupling.qsim.IDSCQSimProvider;
import ch.ethz.idsc.amodeustools.net.DatabaseModule;
import ch.ethz.idsc.amodeustools.net.MatsimStaticDatabase;
import ch.ethz.idsc.amodeustools.net.SimulationServer;
import ch.ethz.idsc.amodeustools.options.ScenarioOptions;
import ch.ethz.idsc.amodeustools.traveldata.TravelData;
import ch.ethz.idsc.amodeustools.traveldata.TravelDataGet;
import ch.ethz.idsc.amodeustools.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeustools.virtualnetwork.VirtualNetworkGet;
import ch.ethz.idsc.owly.data.GlobalAssert;
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
        ReferenceFrame referenceFrame = scenarioOptions.getReferenceFrame();
        // Referenceframe needs to be set manually in IDSCOptions.properties
        GlobalAssert.that(!Objects.isNull(referenceFrame));
        // Locationspec needs to be set manually in IDSCOptions.properties
        GlobalAssert.that(!Objects.isNull(scenarioOptions.getLocationSpec()));

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
        // IncludeActTypeOf.zurichConsensus(config);
        // IncludeActTypeOf.artificial(config);

        String outputdirectory = config.controler().getOutputDirectory();
        System.out.println("outputdirectory = " + outputdirectory);

        // load scenario for simulation
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(scenario != null && network != null && population != null);

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
        controler.addOverridingModule(new DynQSimModule<>(IDSCQSimProvider.class));
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

        AnalyzeSummary analyzeSummary = AnalyzeAll.analyzeNow(configFile, outputdirectory, population, referenceFrame);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        //
        // MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        // PerformanceFleetSizeCalculator performanceFleetSizeCalculator = null;
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

        new DataCollector(configFile, outputdirectory, controler, //
                // minimumFleetSizeCalculator, analyzeSummary, network, population, travelData);
                analyzeSummary, network, population, travelData);

        // generate report
        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.from(configFile, outputdirectory);

    }

    public static void clearMemory() {
        // ToDO Clear the memory for the sequential Server such that the RAM is
        // not limited.

    }
}