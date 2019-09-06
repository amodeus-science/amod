/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.fleetconvert;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.QuadTree;

import amod.scenario.population.TripPopulationCreator;
import amod.scenario.readers.TaxiTripsReader;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripmodif.TaxiDataModifier;
import amod.scenario.tripmodif.TripBasedModifier;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.taxitrip.ExportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
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
    protected final TaxiTripsReader tripsReader;
    protected final MatsimAmodeusDatabase db;
    protected final QuadTree<Link> qt;

    public TripFleetConverter(ScenarioOptions scenarioOptions, Network network, //
            TaxiTripFilter filter, TripBasedModifier tripModifier, //
            TaxiDataModifier generalModifier, TaxiTripFilter finalFilters,//
            TaxiTripsReader tripsReader) {
        this.scenarioOptions = scenarioOptions;
        this.network = network;
        this.filter = filter;
        this.modifier = tripModifier;
        this.generalModifier = generalModifier;
        this.finalFilters = finalFilters;
        this.tripsReader = tripsReader;
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


        
        // BELOW NEW VERSION
        GlobalAssert.that(preparedFile.exists());
        System.out.println("Start to clean " + preparedFile.getAbsolutePath() + " data.");
        // read the file
        System.out.println("Reading: " + preparedFile.getAbsolutePath());
        System.out.println("Using:   " + tripsReader.getClass().getSimpleName());
        Stream<TaxiTrip> stream = tripsReader.getTripStream(preparedFile);


        Stream<TaxiTrip> filteredStream = filter.filterStream(stream);

        
        String fileName = FilenameUtils.getBaseName(preparedFile.getPath()) + "_filtered." + //
                FilenameUtils.getExtension(preparedFile.getPath());
        File AAAAAAEEEEOOOoutFile = new File(preparedFile.getParentFile(), fileName);

        /** export the trips to a new .csv file */
        ExportTaxiTrips.toFile(filteredStream, AAAAAAEEEEOOOoutFile);

        /** save unreadable trips somewhere */
        File unreadable = new File(preparedFile.getParentFile(), //
                FilenameUtils.getBaseName(preparedFile.getAbsolutePath()) + "_unreadable." + //
                        FilenameUtils.getExtension(preparedFile.getAbsolutePath()));
        tripsReader.saveUnreadable(unreadable);
        System.out.println("Finished data cleanup.\n\tstored in " + AAAAAAEEEEOOOoutFile.getAbsolutePath());
        GlobalAssert.that(AAAAAAEEEEOOOoutFile.isFile());
        // ABOVE NEW VERSION
        
        
        
        // BELOW OLD VERSION
//        /** filtering the file */
//        File filteredTripsFile = filter.filter(preparedFile);
//        GlobalAssert.that(filteredTripsFile.isFile());
        // ABOVE OLD VERSION

        /** correcting the file */
        File modifiedTripsFile = modifier.modify(AAAAAAEEEEOOOoutFile);
        GlobalAssert.that(modifiedTripsFile.isFile());

        /** creating population based on corrected, filtered file */
        TripPopulationCreator populationCreator = //
                new TripPopulationCreator(processingDir, configFull, network, db, //
                        DATE_TIME_FORMATTER, qt, simulationDate, timeConvert, finalFilters);
        populationCreator.process(modifiedTripsFile);
    }

    public abstract void setFilters();
}
