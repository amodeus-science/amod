/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import amod.scenario.readers.TaxiTripsReader;
import ch.ethz.idsc.amodeus.taxitrip.ExportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public class TaxiTripFilter {
    private final TaxiTripsReader tripsReader;
    private final List<Predicate<TaxiTrip>> filters = new ArrayList<>();

    public TaxiTripFilter(TaxiTripsReader tripsReader) {
        this.tripsReader = tripsReader;
    }

    public final void addFilter(Predicate<TaxiTrip> filter) {
        filters.add(filter);
    }

    public final File filter(File file)//
            throws IOException {
        GlobalAssert.that(file.exists());
        System.out.println("Start to clean " + file.getAbsolutePath() + " data.");
        Stream<TaxiTrip> stream = readFile(file);
        System.out.println("Number of filters: " + filters.size());
        for (Predicate<TaxiTrip> dataFilter : filters) {
            System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");
            stream = stream.filter(dataFilter);// dataFilter.filter(stream, simOptions, network);
        }
        File outFile = writeFile(file, stream);
        /** save unreadable trips somewhere */
        File unreadable = new File(file.getParentFile(), //
                FilenameUtils.getBaseName(file.getAbsolutePath()) + "_unreadable." + //
                        FilenameUtils.getExtension(file.getAbsolutePath()));
        tripsReader.saveUnreadable(unreadable);
        System.out.println("Finished data cleanup.\n\tstored in " + outFile.getAbsolutePath());
        return outFile;
    }

    private Stream<TaxiTrip> readFile(File file) throws IOException {
        System.out.println("Reading: " + file.getAbsolutePath());
        System.out.println("Using:   " + tripsReader.getClass().getSimpleName());
        return tripsReader.getTripStream(file);
    }

    private File writeFile(File inFile, Stream<TaxiTrip> stream) throws IOException {
        String fileName = FilenameUtils.getBaseName(inFile.getPath()) + "_filtered." + //
                FilenameUtils.getExtension(inFile.getPath());
        File outFile = new File(inFile.getParentFile(), fileName);

        /** export the trips to a new .csv file */
        ExportTaxiTrips.toFile(stream, outFile);
        return outFile;
    }
}
