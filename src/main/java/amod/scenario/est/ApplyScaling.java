/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.amodeus.linkspeed.LinkIndex;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedTimeSeries;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Mean;

/* package */ enum ApplyScaling {
    ;

    public static void to(LinkSpeedDataContainer lsData, TaxiTrip trip, Path path, Scalar factor, int dt) {

        for (Link link : path.links) {
            int linkId = LinkIndex.fromLink(link);
            LinkSpeedTimeSeries lsTime = lsData.getLinkSet().get(LinkIndex.fromLink(link));
            /** if no recordings are present, initialize with free speed */
            if (Objects.isNull(lsTime)) {
                double freeSpeed = link.getFreespeed();

                for (int time = 0; time < 108000; time += dt) {
                    lsData.addData(linkId, time, freeSpeed);
                }
            }
            lsTime = lsData.getLinkSet().get(linkId);
            Objects.requireNonNull(lsTime);
            for (int time : lsTime.getRecordedTimes()) {
                int tripEnd = StaticHelper.endTime(trip);
                if (time <= tripEnd) {
                    Scalar speedNow = RealScalar.of(link.getFreespeed());
                    Tensor recorded = lsTime.getSpeedsAt(time);
                    if (Objects.nonNull(recorded))
                        speedNow = (Scalar) Mean.of(recorded);
                    Scalar newSpeed = speedNow.multiply(factor);
                    lsTime.resetSpeed(time, newSpeed.number().doubleValue());
                }
            }
        }
    }
}
