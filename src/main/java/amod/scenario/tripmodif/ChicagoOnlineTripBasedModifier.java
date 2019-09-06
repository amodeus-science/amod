/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.tensor.qty.Quantity;

public class ChicagoOnlineTripBasedModifier extends TripBasedModifier {

    public ChicagoOnlineTripBasedModifier(Random random, Network network, //
            FastLinkLookup fll, File vNetworkExportFile) {

        /** below filter was removed as it causes request spikes at quarter hour intervals,
         * see class for detailed description */
        // addModifier(new ChicagoTripStartTimeResampling(random));
        /** instead the TripStartTimeShiftResampling is used: */
        addModifier(new TripStartTimeShiftResampling(random, Quantity.of(900, "s")));
        addModifier(new OriginDestinationCentroidResampling(random, network, fll, vNetworkExportFile));
    }

}
