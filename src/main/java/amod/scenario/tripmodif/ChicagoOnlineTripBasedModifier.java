package amod.scenario.tripmodif;

import java.util.Random;

public class ChicagoOnlineTripBasedModifier extends TripBasedModifier {

    public ChicagoOnlineTripBasedModifier(Random random) {
        addModifier(new ChicagoTripStartTimeResampling(random));
    }

}
