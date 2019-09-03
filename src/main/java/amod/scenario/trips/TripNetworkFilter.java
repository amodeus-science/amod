/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.trips;

import java.util.function.Predicate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.TaxiTrip;

public class TripNetworkFilter implements Predicate<TaxiTrip> {

    private final ScenarioOptions scenarioOptions;
    private final Network network;

    public TripNetworkFilter(ScenarioOptions scenarioOptions, Network network) {
        this.scenarioOptions = scenarioOptions;
        this.network = network;
    }

    private static boolean inBounds(Coord minCoord, Coord maxCoord, Coord loc) {
        return (loc.getX() >= minCoord.getX() && loc.getX() <= maxCoord.getX() && // in x Coord
                loc.getY() >= minCoord.getY() && loc.getY() <= maxCoord.getY()); // in y Coord
    }

    @Override
    public boolean test(TaxiTrip t) {
        ReferenceFrame rf = scenarioOptions.getLocationSpec().referenceFrame();
        double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        Coord minCoord = rf.coords_toWGS84().transform(new Coord(networkBounds[0], networkBounds[1]));
        Coord maxCoord = rf.coords_toWGS84().transform(new Coord(networkBounds[2], networkBounds[3]));
        if (minCoord.equals(new Coord(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)) && //
                maxCoord.equals(new Coord(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY))) {
            System.err.println("WARN network seems to be empty.");
            return true;
        }
        return inBounds(minCoord, maxCoord, TensorCoords.toCoord(t.pickupLoc))//
                && inBounds(minCoord, maxCoord, TensorCoords.toCoord(t.dropoffLoc));
    }
}
