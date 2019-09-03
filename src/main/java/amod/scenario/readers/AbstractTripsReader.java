/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.readers;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.util.CsvReader;
import ch.ethz.idsc.amodeus.util.CsvReader.Row;
import ch.ethz.idsc.amodeus.util.Duration;
import ch.ethz.idsc.amodeus.util.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

public abstract class AbstractTripsReader {
    private final String delim;
    private final Map<String, Integer> taxiIds = new HashMap<>();

    public AbstractTripsReader(String delim) {
        this.delim = delim;
    }

    public Stream<TaxiTrip> getTripStream(File file) throws IOException {
        final AtomicInteger tripIds = new AtomicInteger(0);
        List<TaxiTrip> list = new LinkedList<>();
        new CsvReader(file, delim).rows(row -> {

            int tripId = tripIds.getAndIncrement();
            if (tripId % 1000 == 0)
                System.out.println("trips: " + tripId);
            try {
                String taxiCode = getTaxiCode(row);
                int taxiId = taxiIds.getOrDefault(taxiCode, taxiIds.size());
                taxiIds.put(taxiCode, taxiId);

                LocalDateTime pickupTime = getStartTime(row);
                LocalDateTime dropoffTime = getEndTime(row);
                Scalar durationCompute = Duration.between(pickupTime, dropoffTime);
                Scalar durationDataset = getDuration(row);

                if (Scalars.lessEquals(Quantity.of(0.1, SI.SECOND), //
                        durationDataset.subtract(durationCompute).abs()))
                    System.err.println("Mismatch between duration recorded in data and computed duration," + //
                    "computed duration using start and end time: " + //
                    pickupTime + " --> " + dropoffTime + " != " + durationDataset);

                TaxiTrip trip = TaxiTrip.of(tripId, Integer.toString(taxiId), //
                        TensorCoords.toTensor(getPickupLocation(row)), TensorCoords.toTensor(getDropoffLocation(row)), //
                        getDistance(row), getWaitingTime(row), pickupTime, dropoffTime);
                list.add(trip);

            } catch (Exception exception) {
                exception.printStackTrace();
                // TODO
                // System.err.println("discard trip " + tripId + ": [" + IntStream.range(0, headers().size()).mapToObj(i -> //
                // headers.get(i) + "=" + line[i]).collect(Collectors.joining(", ")) + "]");
                // return null;
            }
        });
        return list.stream();
    }

    public int getNumberOfTaxis() {
        return taxiIds.size();
    }

    public abstract String getTaxiCode(Row row);

    public abstract LocalDateTime getStartTime(Row row) throws ParseException;

    public abstract LocalDateTime getEndTime(Row row) throws ParseException;

    public abstract Coord getPickupLocation(Row row);

    public abstract Coord getDropoffLocation(Row row);

    public abstract Scalar getDuration(Row row);

    public abstract Scalar getDistance(Row row);

    public abstract Scalar getWaitingTime(Row row);
}
