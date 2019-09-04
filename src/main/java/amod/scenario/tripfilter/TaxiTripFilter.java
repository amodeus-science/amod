/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import amod.scenario.readers.TaxiTripsReader;
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
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile))) {
            String headers = Arrays.stream(TaxiTrip.class.getFields()).map(Field::getName) //
                    .collect(Collectors.joining(";"));
            bufferedWriter.write(headers);
            System.out.println("now entering second part");
            stream.sorted().forEachOrdered(trip -> {
                try {
                    bufferedWriter.newLine();
                    // localId,taxiId,pickupLoc,dropoffLoc,distance,waitTime,pickupDate,dropoffDate,duration
                    String line = "";
                    line = line + trip.localId;
                    line = line + ";" + trip.taxiId;
                    line = line + ";" + trip.pickupLoc;
                    line = line + ";" + trip.dropoffLoc;
                    line = line + ";" + trip.distance;
                    line = line + ";" + trip.waitTime;
                    line = line + ";" + trip.pickupDate;
                    line = line + ";" + trip.dropoffDate;
                    line = line + ";" + trip.duration;
                    // FIXME below
                    // String line = Arrays.stream(trip.getClass().getFields()).map(field -> {
                    // try {
                    // if (field.get(trip) instanceof LocalDateTime)
                    // return LocalDateTime.parse(text, formatter) dateFormat . format((Date) field.get(trip));
                    // return String.valueOf(field.get(trip));
                    // } catch (Exception e) {
                    // return "";
                    // }
                    // }).collect(Collectors.joining(","));
                    bufferedWriter.write(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        return outFile;
    }
}
