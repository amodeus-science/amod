/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

public interface TripModifier {

    /** informs about the taxiTrip, this SHOULD NOT change
     * the {@link TaxiTrip} @param taxiTrip */
    public void notify(TaxiTrip taxiTrip);

    /** @return modified {@link TaxiTrip} of @param taxiTrip */
    public TaxiTrip modify(TaxiTrip taxiTrip);
}
