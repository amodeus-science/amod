package amod.demo.ext;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.dispatcher.parking.AmodeusParkingModule;
import ch.ethz.idsc.amodeus.dispatcher.parking.ParkingCapacityAdapter;
import ch.ethz.idsc.amodeus.dispatcher.parking.ParkingCapacityAmodeus;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;

public class AmodeusParkingModuleConstantSpots extends AmodeusParkingModule {

    private final Long numberSpotsPerLink;
    
    public AmodeusParkingModuleConstantSpots(ScenarioOptions scenarioOptions, Long numberSpotsPerLink) {
        super(scenarioOptions);
        this.numberSpotsPerLink = numberSpotsPerLink;
    }
    
    @Override
    protected ParkingCapacityAmodeus loadSpatialCapacity(Network network, ScenarioOptions scenarioOptions) {
        return new SimpleParkingCapacities(network, numberSpotsPerLink);
    }

    
    private class SimpleParkingCapacities extends ParkingCapacityAdapter{
        public SimpleParkingCapacities(Network network, Long capacity) {
            for (Link link : network.getLinks().values()) {
                capacities.put(link.getId(), capacity);
            }
        }
    }
}
