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
//    private final TaxiTripsReader tripsReader;
    private final List<Predicate<TaxiTrip>> filters = new ArrayList<>();

    public TaxiTripFilter() {
//        public TaxiTripFilter(TaxiTripsReader tripsReader) {
//        this.tripsReader = tripsReader;
    }

    public final void addFilter(Predicate<TaxiTrip> filter) {
        filters.add(filter);
    }

//    private final File filter(File fileXABJANEDUH)//
//            throws IOException {
//
//        GlobalAssert.that(fileXABJANEDUH.exists());
//        System.out.println("Start to clean " + fileXABJANEDUH.getAbsolutePath() + " data.");
//        // read the file
//        System.out.println("Reading: " + fileXABJANEDUH.getAbsolutePath());
//        System.out.println("Using:   " + tripsReader.getClass().getSimpleName());
//        Stream<TaxiTrip> stream = tripsReader.getTripStream(fileXABJANEDUH);
//        
//        
//        // ABOVE MUST GO EXTERNAL
//        Stream<TaxiTrip> filteredStream = filterStream(stream);
//        // BELOW MUST GO EXTERNAL
//
//        String fileName = FilenameUtils.getBaseName(fileXABJANEDUH.getPath()) + "_filtered." + //
//                FilenameUtils.getExtension(fileXABJANEDUH.getPath());
//        File AAAAAAEEEEOOOoutFile = new File(fileXABJANEDUH.getParentFile(), fileName);
//
//        /** export the trips to a new .csv file */
//        ExportTaxiTrips.toFile(filteredStream, AAAAAAEEEEOOOoutFile);
//
//        /** save unreadable trips somewhere */
//        File unreadable = new File(fileXABJANEDUH.getParentFile(), //
//                FilenameUtils.getBaseName(fileXABJANEDUH.getAbsolutePath()) + "_unreadable." + //
//                        FilenameUtils.getExtension(fileXABJANEDUH.getAbsolutePath()));
//        tripsReader.saveUnreadable(unreadable);
//        System.out.println("Finished data cleanup.\n\tstored in " + AAAAAAEEEEOOOoutFile.getAbsolutePath());
//        return AAAAAAEEEEOOOoutFile;
//    }

    public final Stream<TaxiTrip> filterStream(Stream<TaxiTrip> inStream) {
        System.out.println("Number of filters: " + filters.size());
        for (Predicate<TaxiTrip> dataFilter : filters) {
            System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");
            inStream = inStream.filter(dataFilter);
        }
        return inStream;
    }

}
