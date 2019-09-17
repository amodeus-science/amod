/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.fleetconvert;

import java.util.Random;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.chicago.ScenarioConstants;
import amod.scenario.readers.TaxiTripsReader;
import amod.scenario.tripfilter.RandomRemoverFilter;
import amod.scenario.tripfilter.TaxiTripFilter;
import amod.scenario.tripfilter.TripDurationFilter;
import amod.scenario.tripfilter.TripEndTimeFilter;
import amod.scenario.tripfilter.TripMaxSpeedFilter;
import amod.scenario.tripmodif.TaxiDataModifier;
import amod.scenario.tripmodif.TripBasedModifier;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoOnlineTripFleetConverter extends TripFleetConverter {

    public ChicagoOnlineTripFleetConverter(ScenarioOptions scenarioOptions, Network network, //
            TaxiTripFilter filter, TripBasedModifier modifier, //
            TaxiDataModifier generalModifier, TaxiTripFilter finalFilters, //
            TaxiTripsReader tripsReader) {
        super(scenarioOptions, network, filter, modifier, generalModifier, finalFilters, tripsReader);
    }

    @Override
    public void setFilters() {
        /** very short trips present in the data (0[s], 1[s], etc. ) are removed */
        primaryFilter.addFilter(new TripDurationFilter(Quantity.of(10, SI.SECOND), Quantity.of(Double.MAX_VALUE, SI.SECOND)));

        /** trips which are only explainable with speeds well above 85 miles/hour are removed */
        primaryFilter.addFilter(new TripMaxSpeedFilter(network, db, ScenarioConstants.maxAllowedSpeed));

        /** trips which end after the maximum end time are rejected */
        primaryFilter.addFilter(new TripEndTimeFilter(ScenarioConstants.maxEndTime));

        /** removes a percentage of trips randomly, only used for debugging, for full
         * scale a value > 1.0 is used in the second argument. */
        primaryFilter.addFilter(new RandomRemoverFilter(new Random(123), 0.10));

        // TODO add this again if necessary, otherwise remove eventually and delete classes...
        // cleaner.addFilter(new TripStartTimeResampling(15)); // start/end times in 15 min resolution
        // cleaner.addFilter(new TripEndTimeCorrection());
        // cleaner.addFilter(new TripNetworkFilter(scenarioOptions, network));
        // cleaner.addFilter(new TripDistanceRatioFilter(4)); // massive slow down
        // cleaner.addFilter(new TripDistanceFilter(Quantity.of(500, SI.METER), Quantity.of(50000, SI.METER)));
    }

}
