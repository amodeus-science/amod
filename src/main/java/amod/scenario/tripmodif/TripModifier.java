package amod.scenario.tripmodif;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

public interface TripModifier {

    /** @return modified {@link TaxiTrip} of @param taxiTrip */
    public TaxiTrip modify(TaxiTrip taxiTrip);
}
