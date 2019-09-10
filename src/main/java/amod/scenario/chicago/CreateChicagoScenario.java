/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt2matsim.run.Osm2MultimodalNetwork;

import amod.scenario.Pt2MatsimXML;
import amod.scenario.Scenario;
import amod.scenario.ScenarioLabels;
import amod.scenario.fleetconvert.ChicagoOnlineTripFleetConverter;
import amod.scenario.readers.TaxiTripsReader;
import amod.scenario.tripfilter.TaxiTripFilter;
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
import ch.ethz.idsc.amodeus.util.io.Locate;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.io.DeleteDirectory;

/* package */ enum CreateChicagoScenario {
    ;

    private static final AmodeusTimeConvert timeConvert = new AmodeusTimeConvert(ZoneId.of("America/Chicago"));
    private static final Random random = new Random(123);

    /** in @param args[0] working directory (empty directory), this main function will create
     * an AMoDeus scenario based on the Chicago taxi dataset available online.
     * Settings can afterwards be changed in the AmodeusOptions.properties file located
     * in the directory.
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        File workingDir = new File(args[0]);
        setup(workingDir);
        run(workingDir);
        cleanUp(workingDir);
    }

    private static void setup(File workingDir) throws Exception {
        ChicagoGeoInformation.setup();
        /** copy relevant files containing settings for scenario generation */
        File resourcesDir = new File(Locate.repoFolder(CreateChicagoScenario.class, "amod"), "resources/chicagoScenario");
        CopyFiles.now(resourcesDir.getAbsolutePath(), workingDir.getAbsolutePath(), //
                Arrays.asList(new String[] { ScenarioLabels.avFile, ScenarioLabels.config, //
                        ScenarioLabels.pt2MatSettings }),
                true);
        /** AmodeusOptions.properties is not replaced as it might be changed by user during
         * scenario generation process. */
        if (!new File(workingDir, ScenarioLabels.amodeusFile).exists())
            CopyFiles.now(resourcesDir.getAbsolutePath(), workingDir.getAbsolutePath(), //
                    Arrays.asList(new String[] { ScenarioLabels.amodeusFile }), false);
        Pt2MatsimXML.toLocalFileSystem(new File(workingDir, ScenarioLabels.pt2MatSettings), //
                workingDir.getAbsolutePath());
    }

    private static void run(File workingDir) throws Exception {
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
            DeleteDirectory.of(processingdir, 2, 17);
        if (!processingdir.isDirectory())
            processingdir.mkdir();
        CopyFiles.now(workingDir.getAbsolutePath(), processingdir.getAbsolutePath(), //
                Arrays.asList(new String[] { "AmodeusOptions.properties", "config_full.xml", //
                        "network.xml", "network.xml.gz" }));
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingdir, //
                ScenarioOptionsBase.getDefault());

        LocalDate simulationDate = LocalDateConvert.ofOptions(scenarioOptions.getString("date"));

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
        // TODO eventually remove, this did not improve the fit.
        // finalFilters.addFilter(new TripMaxSpeedFilter(network, db, ScenarioConstants.maxAllowedSpeed));
        ChicagoOnlineTripFleetConverter converter = //
                new ChicagoOnlineTripFleetConverter(scenarioOptions, network, primaryFilter, tripModifier, //
                        new CharRemovalModifier("\""), finalTripFilter, tripsReader);
        Scenario.create(workingDir, tripFile, //
                converter, workingDir, processingdir, simulationDate, timeConvert);
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
