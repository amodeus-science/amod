package amod.scenario.tripmodif;

import java.io.File;
import java.util.Random;

public class ChicagoOnlineTripBasedModifier extends TripBasedModifier {

    public ChicagoOnlineTripBasedModifier(Random random) {
        addModifier(new ChicagoTripStartTimeResampling(random));
        addModifier(new OriginDestinationCentroidResampling(random));
    }

}
