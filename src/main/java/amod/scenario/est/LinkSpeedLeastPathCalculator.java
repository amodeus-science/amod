/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.amodeus.linkspeed.LSDataTravelTime;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;

/* package */ enum LinkSpeedLeastPathCalculator {
    ;

    public static LeastCostPathCalculator from(Network network, //
            MatsimAmodeusDatabase db, LinkSpeedDataContainer lsData) {
        TravelTime travelTime = new LSDataTravelTime(lsData, db);
        return new DijkstraFactory().createPathCalculator(network, //
                new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }

}
