/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.fleetconvert;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.readers.TaxiTripsReader;
import amod.scenario.tripfilter.DeprcTripDistanceFilter;
import amod.scenario.tripfilter.DeprcTripNetworkFilter;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripfilter.TripDurationFilter;
import amod.scenario.tripmodif.TaxiDataModifier;
import amod.scenario.tripmodif.TripBasedModifier;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoTripFleetConverter extends TripFleetConverter {

    public ChicagoTripFleetConverter(ScenarioOptions scenarioOptions, Network network, TaxiTripFilter cleaner, //
            TripBasedModifier corrector, TaxiDataModifier generalModifier, TaxiTripFilter finalFilters, //
            TaxiTripsReader tripsReader) {
        super(scenarioOptions, network, cleaner, corrector, generalModifier, finalFilters, tripsReader);
    }

    @Override
    public void setFilters() {
        // TODO trips were redistributed in 15 minutes interval randomly before,
        // add this again if necessary...
        primaryFilter.addFilter(new DeprcTripNetworkFilter(scenarioOptions, network));
        primaryFilter.addFilter(new TripDurationFilter(Quantity.of(0, SI.SECOND), Quantity.of(20000, SI.SECOND)));
        primaryFilter.addFilter(new DeprcTripDistanceFilter(Quantity.of(500, SI.METER), Quantity.of(50000, SI.METER)));
    }

}
