package amod.scenario.tripmodif;

import java.util.HashSet;
import java.util.Random;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.Tensor;

public class OriginDestinationCentroidResampling implements TripModifier {

    private final Random random;
    private final HashSet<Tensor> uniqueOrigins = new HashSet<>();
    private final HashSet<Tensor> uniqueDestins = new HashSet<>();
    private final HashSet<Tensor> uniqueLocations = new HashSet<>();

    public OriginDestinationCentroidResampling(Random random) {
        this.random = random;
    }

    @Override
    public TaxiTrip modify(TaxiTrip taxiTrip) {
        TaxiTrip orig = taxiTrip;
        return orig;
    }

    @Override
    public void notify(TaxiTrip taxiTrip) {
        uniqueOrigins.add(taxiTrip.pickupLoc);
        uniqueDestins.add(taxiTrip.dropoffLoc);
        uniqueLocations.add(taxiTrip.pickupLoc);
        uniqueLocations.add(taxiTrip.dropoffLoc);

        System.out.println("uniqueOrigins: " + uniqueOrigins.size());
        System.out.println("uniqueDestins: " + uniqueDestins.size());
        System.out.println("uniqueLocations: " + uniqueLocations.size());

    }

}
