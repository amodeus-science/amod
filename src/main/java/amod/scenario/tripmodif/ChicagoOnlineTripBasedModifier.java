package amod.scenario.tripmodif;

import java.io.File;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;

import amod.scenario.tripfilter.TripMaxSpeedFilter;
import ch.ethz.idsc.amodeus.net.FastLinkLookup;

public class ChicagoOnlineTripBasedModifier extends TripBasedModifier {

    public ChicagoOnlineTripBasedModifier(Random random, Network network, //
            FastLinkLookup fll, File vNetworkExportFile) {
        addModifier(new ChicagoTripStartTimeResampling(random));
        addModifier(new OriginDestinationCentroidResampling(random, network, fll, vNetworkExportFile));
    }

}
