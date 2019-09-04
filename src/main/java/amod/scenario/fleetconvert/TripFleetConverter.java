/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.fleetconvert;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.QuadTree;

import amod.scenario.dataclean.DataCorrector;
import amod.scenario.dataclean.TripDataCleaner;
import amod.scenario.population.TripPopulationCreator;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.math.CreateQuadTree;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public abstract class TripFleetConverter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = //
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    protected final ScenarioOptions scenarioOptions;
    protected final Network network;
    protected final TripDataCleaner cleaner;
    protected final DataCorrector corrector;

    public TripFleetConverter(ScenarioOptions scenarioOptions, Network network, //
            TripDataCleaner cleaner, DataCorrector corrector) {
        this.scenarioOptions = scenarioOptions;
        this.network = network;
        this.cleaner = cleaner;
        this.corrector = corrector;
    }

    public void run(File processingDir, File tripFile, LocalDate simulationDate, AmodeusTimeConvert timeConvert)//
            throws Exception {
        GlobalAssert.that(tripFile.isFile());

        /** preparation of necessary data */
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        GlobalAssert.that(!network.getNodes().isEmpty());
        System.out.println("INFO working folder: " + processingDir.getAbsolutePath());
        ReferenceFrame referenceFrame = scenarioOptions.getLocationSpec().referenceFrame();
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);

        /** folder for processing stored files, the folder tripData contains
         * .csv versions of all processing steps for faster debugging. */
        File newWorkingDir = new File(processingDir, "tripData");
        newWorkingDir.mkdirs();
        FileUtils.copyFileToDirectory(tripFile, newWorkingDir);
        File newTripFile = new File(newWorkingDir, tripFile.getName());
        GlobalAssert.that(newTripFile.isFile());

        /** correcting the file */
        File correctedTripFile = corrector.correctFile(newTripFile, db);
        GlobalAssert.that(correctedTripFile.isFile());

        /** filtering the file */
        File cleanTripFile = cleaner.clean(correctedTripFile);
        GlobalAssert.that(cleanTripFile.isFile());

        /** creating population based on corrected, filtered file */
        QuadTree<Link> qt = CreateQuadTree.of(network, db);
        TripPopulationCreator populationCreator = //
                new TripPopulationCreator(processingDir, configFull, network, db, //
                        DATE_TIME_FORMATTER, qt, simulationDate, timeConvert);
        populationCreator.process(cleanTripFile);
    }

    public abstract void setFilters();
}
