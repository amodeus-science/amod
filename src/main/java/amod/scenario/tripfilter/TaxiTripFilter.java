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

/** Contains a set of filters that process an individual {@link TaxiTrip}
 * and let it pass or not: TaxiTrip -> {true,false} */
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
        // read the file
        System.out.println("Reading: " + file.getAbsolutePath());
        System.out.println("Using:   " + tripsReader.getClass().getSimpleName());
        Stream<TaxiTrip> stream = tripsReader.getTripStream(file);
        Stream<TaxiTrip> filteredStream = filterStream(stream);
        
//        System.out.println("Number of filters: " + filters.size());
//        for (Predicate<TaxiTrip> dataFilter : filters) {
//            System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");
//            stream = stream.filter(dataFilter);
//        }
        
        
        File outFile = writeFile(file, filteredStream);
        /** save unreadable trips somewhere */
        File unreadable = new File(file.getParentFile(), //
                FilenameUtils.getBaseName(file.getAbsolutePath()) + "_unreadable." + //
                        FilenameUtils.getExtension(file.getAbsolutePath()));
        tripsReader.saveUnreadable(unreadable);
        System.out.println("Finished data cleanup.\n\tstored in " + outFile.getAbsolutePath());
        return outFile;
    }
    
    private final Stream<TaxiTrip> filterStream(Stream<TaxiTrip> inStream){
        System.out.println("Number of filters: " + filters.size());
        for (Predicate<TaxiTrip> dataFilter : filters) {
            System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");
            inStream = inStream.filter(dataFilter);
        }
        return inStream;        
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
