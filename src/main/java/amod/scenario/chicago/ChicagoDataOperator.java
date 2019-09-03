/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.scenario.DataOperator;
import ch.ethz.idsc.amodeus.scenario.dataclean.StandardDataCorrector;
import ch.ethz.idsc.amodeus.scenario.dataclean.TripDataCleaner;
import ch.ethz.idsc.amodeus.scenario.fleetconvert.TripFleetConverter;
import ch.ethz.idsc.amodeus.scenario.trips.TaxiTrip;
import ch.ethz.idsc.amodeus.scenario.trips.TripDistanceFilter;
import ch.ethz.idsc.amodeus.scenario.trips.TripDurationFilter;
import ch.ethz.idsc.amodeus.scenario.trips.TripNetworkFilter;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoDataOperator extends DataOperator<TaxiTrip> {

    public ChicagoDataOperator(ScenarioOptions scenarioOptions, Network network) {
        super(new TripFleetConverter(), new StandardDataCorrector(), //
                new TripDataCleaner(new TripsReaderChicago()), //
                scenarioOptions, network);
    }

    @Override
    public void setFilters() {
        // TODO trips were redistributed in 15 minutes interval randomly before,
        // add this again if necessary...
        // cleaner.addFilter(new TripStartTimeResampling(15)); // start/end times in 15 min resolution
        // cleaner.addFilter(new TripEndTimeCorrection());
        cleaner.addFilter(new TripNetworkFilter(scenarioOptions, network));
        // cleaner.addFilter(new TripDistanceRatioFilter(4)); // massive slow down
        cleaner.addFilter(new TripDurationFilter(Quantity.of(20000, SI.SECOND)));
        cleaner.addFilter(new TripDistanceFilter(Quantity.of(500, SI.METER), Quantity.of(50000, SI.METER)));
    }

}
