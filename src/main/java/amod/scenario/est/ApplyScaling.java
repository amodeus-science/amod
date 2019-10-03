/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.linkspeed.LinkIndex;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedTimeSeries;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;

/* package */ enum ApplyScaling {
    ;

    public static void to(LinkSpeedDataContainer lsData, TaxiTrip trip, Path path, //
            Scalar rescalefactor, int dt) {
        int tripStart = StaticHelper.startTime(trip);
        int tripEnd = StaticHelper.endTime(trip);

        for (Link link : path.links) {
            /** get link properties */
            String linkId = LinkIndex.fromLink(link);
            double freeSpeed = link.getFreespeed();
            LinkSpeedTimeSeries lsTime = lsData.getLinkSet().get(linkId);

            /** if no recordings are present, initialize with free speed for
             * duration of trip */
            if (Objects.isNull(lsTime)) {
                // for (int time = tripStart; time <= tripEnd; time += dt) {
                // lsData.addData(linkId, time, freeSpeed);
                // }
                // TODO remove magic const. really necessary all day?
                for (int time = 0; time <= 108000; time += dt) {
                    lsData.addData(linkId, time, freeSpeed);
                }
            }
            lsTime = lsData.getLinkSet().get(linkId);
            Objects.requireNonNull(lsTime);

            List<Integer> relevantTimes = new ArrayList<>();
            for (int time : lsTime.getRecordedTimes()) {
                if (tripStart <= time && time <= tripEnd) {
                    relevantTimes.add(time);
                }
            }
            if (relevantTimes.size() == 0) // must have at least one entry for convergence
                relevantTimes.add(lsTime.getTimeFloor(tripStart));

            GlobalAssert.that(relevantTimes.size() > 0);

            for (int time : lsTime.getRecordedTimes()) {
                Scalar speedNow = RealScalar.of(freeSpeed);
                Double recorded = lsTime.getSpeedsAt(time);
                if (Objects.nonNull(recorded))
                    speedNow = RealScalar.of(recorded);
                Scalar newSpeedS = speedNow.multiply(rescalefactor);
                double newSpeed = newSpeedS.number().doubleValue();

                // NOW
                lsTime.setSpeed(time, newSpeed);
            }
        }
    }
}
