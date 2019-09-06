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

import amod.scenario.population.TripPopulationCreator;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripmodif.TaxiDataModifier;
import amod.scenario.tripmodif.TripBasedModifier;
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
    protected final TaxiTripFilter filter;
    protected final TripBasedModifier modifier;
    protected final TaxiDataModifier generalModifier;
    protected final TaxiTripFilter finalFilters;
    protected final MatsimAmodeusDatabase db;
    protected final QuadTree<Link> qt;

    public TripFleetConverter(ScenarioOptions scenarioOptions, Network network, //
            TaxiTripFilter filter, TripBasedModifier tripModifier, //
            TaxiDataModifier generalModifier, TaxiTripFilter finalFilters) {
        this.scenarioOptions = scenarioOptions;
        this.network = network;
        this.filter = filter;
        this.modifier = tripModifier;
        this.generalModifier = generalModifier;
        this.finalFilters = finalFilters;
        ReferenceFrame referenceFrame = scenarioOptions.getLocationSpec().referenceFrame();
        this.db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        this.qt = CreateQuadTree.of(network);
    }

    public void run(File processingDir, File tripFile, LocalDate simulationDate, AmodeusTimeConvert timeConvert)//
            throws Exception {
        GlobalAssert.that(tripFile.isFile());

        /** preparation of necessary data */
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        System.out.println("INFO working folder: " + processingDir.getAbsolutePath());

        /** folder for processing stored files, the folder tripData contains
         * .csv versions of all processing steps for faster debugging. */
        File newWorkingDir = new File(processingDir, "tripData");
        newWorkingDir.mkdirs();
        FileUtils.copyFileToDirectory(tripFile, newWorkingDir);
        File newTripFile = new File(newWorkingDir, tripFile.getName());
        GlobalAssert.that(newTripFile.isFile());

        /** initial modifications, e.g., replacing characters, all
         * other modifications should be done in the third step */
        File preparedFile = generalModifier.modify(newTripFile);

        /** filtering the file */
        File filteredTripsFile = filter.filter(preparedFile);
        GlobalAssert.that(filteredTripsFile.isFile());

        /** correcting the file */
        File modifiedTripsFile = modifier.modify(filteredTripsFile);
        GlobalAssert.that(modifiedTripsFile.isFile());

        /** creating population based on corrected, filtered file */
        TripPopulationCreator populationCreator = //
                new TripPopulationCreator(processingDir, configFull, network, db, //
                        DATE_TIME_FORMATTER, qt, simulationDate, timeConvert, finalFilters);
        populationCreator.process(modifiedTripsFile);
    }

    public abstract void setFilters();
}
