/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.chicago;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.DataOperator;
import amod.scenario.dataclean.CharRemovalDataCorrector;
import amod.scenario.dataclean.TripDataCleaner;
import amod.scenario.fleetconvert.TripFleetConverter;
import amod.scenario.trips.TripDistanceFilter;
import amod.scenario.trips.TripDurationFilter;
import amod.scenario.trips.TripNetworkFilter;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoOnlineDataOperator extends DataOperator<TaxiTrip> {

    public ChicagoOnlineDataOperator(ScenarioOptions scenarioOptions, Network network) {
        super(new TripFleetConverter(), new CharRemovalDataCorrector("\""), //
                new TripDataCleaner(new OnlineTripsReaderChicago()), //
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
