/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.util.CsvReader.Row;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;

public class TripsReaderChicago extends ChicagoTripsReaderBasic {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss a");

    public TripsReaderChicago() {
        super(",");

    }

    @Override
    public final String getTaxiCode(Row row) {
        return row.get("Taxi ID");
    }

    @Override
    public LocalDateTime getStartTime(Row line) throws ParseException {
        return LocalDateTime.parse(line.get("Trip Start Timestamp"), DATE_TIME_FORMATTER);
    }

    @Override
    public LocalDateTime getEndTime(Row line) throws ParseException {
        return LocalDateTime.parse(line.get("Trip End Timestamp"), DATE_TIME_FORMATTER);
    }

    @Override
    public Tensor getPickupLocation(Row line) {
        Tensor loc = Tensors.vector(Double.valueOf(line.get("Pickup Centroid Longitude")),//
                Double.valueOf(line.get("Pickup Centroid Latitude")));
        return loc;
    }

    @Override
    public Tensor getDropoffLocation(Row line) {
        Tensor loc = Tensors.vector(Double.valueOf(line.get("Dropoff Centroid Longitude")),//
                Double.valueOf(line.get("Dropoff Centroid Latitude")));
        return loc;
    }

    @Override
    public Scalar getDuration(Row line) {
        return Quantity.of(Long.valueOf(line.get("Trip Seconds")), SI.SECOND);
    }

}
