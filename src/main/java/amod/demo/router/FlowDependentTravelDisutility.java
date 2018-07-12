package amod.demo.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class FlowDependentTravelDisutility implements TravelDisutility {

    private static final Logger log = Logger.getLogger(FlowDependentTravelDisutility.class);
    
    protected final TravelTime travelTime;

    public FlowDependentTravelDisutility(final TravelTime travelTime) {
        if (travelTime == null) {
            log.warn("TimeCalculator is null so FreeSpeedTravelTimes will be calculated!");
            this.travelTime = new FreeSpeedTravelTime();
        } else this.travelTime = travelTime;
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {     
        return this.travelTime.getLinkTravelTime(link, time, person, vehicle);
    }

    @Override
    public double getLinkMinimumTravelDisutility(final Link link) {
        return this.travelTime.getLinkTravelTime(link, Time.UNDEFINED_TIME, null, null);
    }
    
}