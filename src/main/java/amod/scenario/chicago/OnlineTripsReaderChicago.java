/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import java.text.ParseException;
import java.time.LocalDateTime;

import ch.ethz.idsc.amodeus.util.CsvReader.Row;
import ch.ethz.idsc.amodeus.util.LocalDateTimes;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;

public class OnlineTripsReaderChicago extends ChicagoTripsReaderBasic {

    public OnlineTripsReaderChicago() {
        super(",");
    }

    @Override
    public final String getTaxiCode(Row row) {
        return row.get("taxi_id");
    }

    @Override
    public LocalDateTime getStartTime(Row line) throws ParseException {
        return LocalDateTime.parse(line.get("trip_start_timestamp"), ScenarioConstants.onlineFormat);
    }

    @Override
    public LocalDateTime getEndTime(Row line) throws ParseException {
        return LocalDateTimes.addTo(getStartTime(line), getDuration(line));
        // return LocalDateTime.parse(line.get("trip_end_timestamp"), ScenarioConstants.onlineFormat);
    }

    @Override
    public Tensor getPickupLocation(Row line) {
        Tensor loc = Tensors.vector(Double.valueOf(line.get("pickup_centroid_longitude")), //
                Double.valueOf(line.get("pickup_centroid_latitude")));
        return loc;
    }

    @Override
    public Tensor getDropoffLocation(Row line) {
        Tensor loc = Tensors.vector(Double.valueOf(line.get("dropoff_centroid_longitude")), //
                Double.valueOf(line.get("dropoff_centroid_latitude")));
        return loc;
    }

    @Override
    public Scalar getDuration(Row line) {
        return Quantity.of(Long.valueOf(line.get("trip_seconds")), SI.SECOND);
    }

}

// TODO move to tests
// public static void main(String[] args) throws ParseException {
// String dateString = "2018-01-22T22:17:25.123";
//
// /** old Date stuff */
// DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
// Date date = format.parse(dateString);
// System.out.println(date);
// /** LocalDateTime */
// DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
// LocalDateTime ldt = LocalDateTime.parse(dateString, dtf);// dtf.parse(dateString).;
// System.out.println(ldt);
//
// }
