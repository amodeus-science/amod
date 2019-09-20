/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import ch.ethz.idsc.amodeus.linkspeed.LinkIndex;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedTimeSeries;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.red.Mean;

/* package */ class LSDataTravelTime implements TravelTime {

    private final LinkSpeedDataContainer lsData;

    public LSDataTravelTime(LinkSpeedDataContainer lsData) {
        this.lsData = lsData;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        Integer linkID = LinkIndex.fromLink(link);
        Scalar speed = RealScalar.of(link.getFreespeed());

        LinkSpeedTimeSeries timeSeries = lsData.getLinkSet().get(linkID);
        if (Objects.nonNull(timeSeries)) {
            LinkSpeedTimeSeries series = lsData.getLinkSet().get(linkID);
            Objects.requireNonNull(series);
            speed = (Scalar) Mean.of(series.getSpeedsFloor((int) time));
        }

        return link.getLength() / speed.number().doubleValue();
    }

}
