/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import ch.ethz.idsc.amodeus.scenario.DataOperator;
import ch.ethz.idsc.amodeus.scenario.dataclean.CharRemovalDataCorrector;
import ch.ethz.idsc.amodeus.scenario.dataclean.TripDataCleaner;
import ch.ethz.idsc.amodeus.scenario.fleetconvert.TripFleetConverter;
import ch.ethz.idsc.amodeus.scenario.trips.TaxiTrip;
import ch.ethz.idsc.amodeus.scenario.trips.TripDistanceFilter;
import ch.ethz.idsc.amodeus.scenario.trips.TripDurationFilter;
import ch.ethz.idsc.amodeus.scenario.trips.TripNetworkFilter;
import ch.ethz.idsc.amodeus.scenario.trips.TripStartTimeResampling;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoOnlineDataOperator extends DataOperator<TaxiTrip> {

    public ChicagoOnlineDataOperator() {
        super(new TripFleetConverter(), new CharRemovalDataCorrector("\""), new TripDataCleaner(new OnlineTripsReaderChicago()));
    }

    @Override
    public void setFilters() {
        cleaner.addFilter(new TripStartTimeResampling(15)); // start/end times in 15 min resolution
        // cleaner.addFilter(new TripEndTimeCorrection());
        cleaner.addFilter(new TripNetworkFilter());
        // cleaner.addFilter(new TripDistanceRatioFilter(4)); // massive slow down
        cleaner.addFilter(new TripDurationFilter(Quantity.of(20000, SI.SECOND)));
        cleaner.addFilter(new TripDistanceFilter(Quantity.of(500, SI.METER), Quantity.of(50000, SI.METER)));
    }

}
