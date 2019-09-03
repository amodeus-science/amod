/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt2matsim.run.Osm2MultimodalNetwork;

import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.scenario.OsmLoader;
import ch.ethz.idsc.amodeus.scenario.Pt2MatsimXML;
import ch.ethz.idsc.amodeus.scenario.ScenarioCreator;
import ch.ethz.idsc.amodeus.scenario.ScenarioLabels;
import ch.ethz.idsc.amodeus.util.io.CopyFiles;
import ch.ethz.idsc.amodeus.util.io.LocateUtils;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.io.DeleteDirectory;

/* package */ enum CreateChicagoScenario {
    ;

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

    public static void setup(File workingDir) throws Exception {
        ChicagoGeoInformation.setup();
        /** copy relevant files containing settings for scenario generation */
        File settingsDir = new File(LocateUtils.getSuperFolder("amodeus"), "resources/chicagoScenario");
        CopyFiles.now(settingsDir.getAbsolutePath(), workingDir.getAbsolutePath(), //
                Arrays.asList(new String[] { ScenarioLabels.avFile, ScenarioLabels.config, //
                        ScenarioLabels.pt2MatSettings }),
                true);
        /** AmodeusOptions.properties is not replaced as it might be changed by user during
         * scenario generation process. */
        if (!new File(workingDir, ScenarioLabels.amodeusFile).exists())
            CopyFiles.now(settingsDir.getAbsolutePath(), workingDir.getAbsolutePath(), //
                    Arrays.asList(new String[] { ScenarioLabels.amodeusFile }), false);
        Pt2MatsimXML.toLocalFileSystem(new File(workingDir, ScenarioLabels.pt2MatSettings), //
                workingDir.getAbsolutePath());
    }

    public static void run(File workingDir) throws Exception {
        /** download of open street map data to create scenario */
        System.out.println("Downloading open stret map data, this may take a while...");
        File osmFile = new File(workingDir, ScenarioLabels.osmData);
        OsmLoader osm = new OsmLoader(new File(workingDir, ScenarioLabels.amodeusFile));
        osm.saveIfNotAlreadyExists(osmFile);
        /** generate a network using pt2Matsim */
        Osm2MultimodalNetwork.run(workingDir.getAbsolutePath() + "/" + ScenarioLabels.pt2MatSettings);
        /** based on the taxi data, create a population and assemble a AMoDeus scenario */
        File taxiData = ChicagoDataLoader.from(ScenarioLabels.amodeusFile, workingDir);
        File processingdir = new File(workingDir, "Scenario");
        if (processingdir.isDirectory())
            DeleteDirectory.of(processingdir, 1, 4);
        if (!processingdir.isDirectory())
            processingdir.mkdir();
        CopyFiles.now(workingDir.getAbsolutePath(), processingdir.getAbsolutePath(), //
                Arrays.asList(new String[] { "AmodeusOptions.properties", "config_full.xml", "network.xml" }));
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingdir, //
                ScenarioOptionsBase.getDefault());
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        System.out.println(configFile.getAbsolutePath());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        Network network = NetworkLoader.fromNetworkFile(new File(processingdir, configFull.network().getInputFile())); // loadNetwork(configFile);
        ScenarioCreator scenarioCreator = new ScenarioCreator(workingDir, taxiData, //
                new ChicagoOnlineDataOperator(scenarioOptions, network), workingDir, //
                scenarioOptions, processingdir, network, "trip_id");
    }

    public static void cleanUp(File workingDir) throws IOException {
        /** delete unneeded files */
        DeleteDirectory.of(new File(workingDir, "Scenario"), 2, 14);
        DeleteDirectory.of(new File(workingDir, ScenarioLabels.amodeusFile), 0, 1);
        DeleteDirectory.of(new File(workingDir, ScenarioLabels.avFile), 0, 1);
        DeleteDirectory.of(new File(workingDir, ScenarioLabels.config), 0, 1);
        DeleteDirectory.of(new File(workingDir, ScenarioLabels.pt2MatSettings), 0, 1);
        DeleteDirectory.of(new File(workingDir, ScenarioLabels.network), 0, 1);
    }
}
