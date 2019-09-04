/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;

import amod.scenario.tripfilter.TaxiTripFilter;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;

public interface TaxiDataModifier {

    /** @return a new {@link File} containing taxi trip data, which is derived
     *         from the original {@link File} @param taxiData. Modifications are
     *         necessary changes in the data to produce meaningful scenarios, e.g.,
     *         - distributing trips deparing in 15 minute intervals in these intervals
     *         - distributing trips in geographical areas, e.g., if only the departure zone is known
     *         Possible parameters: @param db
     * 
     *         Filtering, i.e., removing certain trips according to some criteria is done
     *         with a {@link TaxiTripFilter} and not with classes implementing this interface.
     * 
     * @throws Exception */
    public File modify(File taxiData, MatsimAmodeusDatabase db) throws Exception;

}
