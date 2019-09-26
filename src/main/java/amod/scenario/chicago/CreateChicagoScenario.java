/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt2matsim.run.Osm2MultimodalNetwork;

import amod.scenario.FinishedScenario;
import amod.scenario.Scenario;
import amod.scenario.ScenarioLabels;
import amod.scenario.est.IterativeLinkSpeedEstimator;
import amod.scenario.fleetconvert.ChicagoOnlineTripFleetConverter;
import amod.scenario.readers.TaxiTripsReader;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripfilter.TripNetworkFilter;
import amod.scenario.tripmodif.CharRemovalModifier;
import amod.scenario.tripmodif.ChicagoOnlineTripBasedModifier;
import amod.scenario.tripmodif.TripBasedModifier;
import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.OsmLoader;
import ch.ethz.idsc.amodeus.util.io.CopyFiles;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.io.DeleteDirectory;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ class CreateChicagoScenario {

    /** in @param args[0] working directory (empty directory), this main function will create
     * an AMoDeus scenario based on the Chicago taxi dataset available online.
     * Settings can afterwards be changed in the AmodeusOptions.properties file located
     * in the directory.
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        File workingDir = new File(args[0]);
        new CreateChicagoScenario(workingDir);
    }

    // --
    private static final AmodeusTimeConvert timeConvert = new AmodeusTimeConvert(ZoneId.of("America/Chicago"));
    private static final Random random = new Random(123);
    private final File workingDir;
    private final File processingDir;
    private File finalTripsFile;

    private CreateChicagoScenario(File workingDir) throws Exception {
        this.workingDir = workingDir;
        ChicagoSetup.in(workingDir);
        processingDir = run();
        File destinDir = new File(workingDir, "CreatedScenario");
        Objects.requireNonNull(finalTripsFile);

        System.out.println("The final trips file is: ");
        System.out.println(finalTripsFile.getAbsolutePath());

        // this is the old LP-based code
        // ChicagoLinkSpeeds.compute(processingDir, finalTripsFile);
        // new code
        new IterativeLinkSpeedEstimator().compute(processingDir, finalTripsFile);

        FinishedScenario.copyToDir(workingDir.getAbsolutePath(), processingDir.getAbsolutePath(), //
                destinDir.getAbsolutePath());
        cleanUp(workingDir);
    }

    private File run() throws Exception {
        // FIXME remove debug loop once done
        boolean debug = true;

        /** download of open street map data to create scenario */
        System.out.println("Downloading open stret map data, this may take a while...");
        File osmFile = new File(workingDir, ScenarioLabels.osmData);
        OsmLoader osm = new OsmLoader(new File(workingDir, ScenarioLabels.amodeusFile));
        osm.saveIfNotAlreadyExists(osmFile);
        /** generate a network using pt2Matsim */
        if (!debug)
            Osm2MultimodalNetwork.run(workingDir.getAbsolutePath() + "/" + ScenarioLabels.pt2MatSettings);
        /** prepare the network */
        InitialNetworkPreparer.run(workingDir);

        /** based on the taxi data, create a population and assemble a AMoDeus scenario */

        File tripFile;
        if (!debug) {
            tripFile = ChicagoDataLoader.from(ScenarioLabels.amodeusFile, workingDir);
        } else {
            tripFile = new File("/home/clruch/data/TaxiComparison_ChicagoScCr/Taxi_Trips_2019_07_19.csv");
        }

        File processingdir = new File(workingDir, "Scenario");
        if (processingdir.isDirectory())
            DeleteDirectory.of(processingdir, 2, 21);
        if (!processingdir.isDirectory())
            processingdir.mkdir();
        CopyFiles.now(workingDir.getAbsolutePath(), processingdir.getAbsolutePath(), //
                Arrays.asList(new String[] { "AmodeusOptions.properties", "config_full.xml", //
                        "network.xml", "network.xml.gz", "LPOptions.properties" }));
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingdir, //
                ScenarioOptionsBase.getDefault());
        LocalDate simulationDate = LocalDateConvert.ofOptions(scenarioOptions.getString("date"));

        //
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        System.out.println(configFile.getAbsolutePath());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        Network network = NetworkLoader.fromNetworkFile(new File(processingdir, configFull.network().getInputFile()));
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, scenarioOptions.getLocationSpec().referenceFrame());
        FastLinkLookup fll = new FastLinkLookup(network, db);

        /** prepare for creation of scenario */
        TaxiTripsReader tripsReader = new OnlineTripsReaderChicago();
        TaxiTripFilter primaryFilter = new TaxiTripFilter();
        TripBasedModifier tripModifier = new ChicagoOnlineTripBasedModifier(random, network, //
                fll, new File(processingdir, "virtualNetworkChicago"));
        TaxiTripFilter finalTripFilter = new TaxiTripFilter();
        /** trips which are faster than the network freeflow speeds would allow are removed */
        finalTripFilter.addFilter(new TripNetworkFilter(network, db,//
                Quantity.of(5.5, "m*s^-1"), Quantity.of(3600, "s"), Quantity.of(200, "m")));

        // TODO eventually remove, this did not improve the fit.
        // finalFilters.addFilter(new TripMaxSpeedFilter(network, db, ScenarioConstants.maxAllowedSpeed));
        ChicagoOnlineTripFleetConverter converter = //
                new ChicagoOnlineTripFleetConverter(scenarioOptions, network, primaryFilter, tripModifier, //
                        new CharRemovalModifier("\""), finalTripFilter, tripsReader);
        finalTripsFile = Scenario.create(workingDir, tripFile, //
                converter, workingDir, processingdir, simulationDate, timeConvert);
        return processingdir;
    }

    private static void cleanUp(File workingDir) throws IOException {
        /** delete unneeded files */
        // DeleteDirectory.of(new File(workingDir, "Scenario"), 2, 14);
        // DeleteDirectory.of(new File(workingDir, ScenarioLabels.amodeusFile), 0, 1);
        // DeleteDirectory.of(new File(workingDir, ScenarioLabels.avFile), 0, 1);
        // DeleteDirectory.of(new File(workingDir, ScenarioLabels.config), 0, 1);
        // DeleteDirectory.of(new File(workingDir, ScenarioLabels.pt2MatSettings), 0, 1);
        // DeleteDirectory.of(new File(workingDir, ScenarioLabels.network), 0, 1);
    }
}
