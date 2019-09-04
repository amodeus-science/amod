package amod.scenario.fleetconvert;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.dataclean.TaxiDataModifier;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripfilter.TripDurationFilter;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoOnlineTripFleetConverter extends TripFleetConverter {

    public ChicagoOnlineTripFleetConverter(ScenarioOptions scenarioOptions, Network network, TaxiTripFilter cleaner, //
            TaxiDataModifier corrector) {
        super(scenarioOptions, network, cleaner, corrector);
    }

    @Override
    public void setFilters() {
        // TODO trips were redistributed in 15 minutes interval randomly before,
        // add this again if necessary...
        // cleaner.addFilter(new TripStartTimeResampling(15)); // start/end times in 15 min resolution
        // cleaner.addFilter(new TripEndTimeCorrection());
        // cleaner.addFilter(new TripNetworkFilter(scenarioOptions, network));
        // cleaner.addFilter(new TripDistanceRatioFilter(4)); // massive slow down
        // cleaner.addFilter(new TripDurationFilter(Quantity.of(20000, SI.SECOND)));
        // cleaner.addFilter(new TripDistanceFilter(Quantity.of(500, SI.METER), Quantity.of(50000, SI.METER)));
    }

}
