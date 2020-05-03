/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.VehicleType;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.matsim.mod.RandomDensityGenerator;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.data.AVVehicle;
import ch.ethz.matsim.av.generator.AVGenerator;
import ch.ethz.matsim.av.generator.AVUtils;

/** the initial placement of {@link RoboTaxi} in the {@link Network} is determined
 * with an {@link AVGenerator}. In most cases it is sufficient to use the
 * {@link RandomDensityGenerator} provided in AMoDeus, however, users may wish
 * to have an initial placement of {@link RoboTaxi} determined by themselves.
 * This class demonstrates such a placement in which 10 random links are chosen
 * and all vehicles are placed on these random links. */
public class DemoGenerator implements AVGenerator {
    private static final Logger LOGGER = Logger.getLogger(DemoGenerator.class);
    // ---
    private final VehicleType vehicleType;
    private final OperatorConfig operatorConfig;
    private final Collection<Link> randomLinks = new ArrayList<>();

    public DemoGenerator(OperatorConfig operatorConfig, Network network, VehicleType vehicleType) {
        this.operatorConfig = operatorConfig;
        this.vehicleType = vehicleType;

        /** determine 10 random links in the network */
        int bound = network.getLinks().size();
        for (int i = 0; i < 10; ++i) {
            int elemRand = MatsimRandom.getRandom().nextInt(bound);
            Link link = network.getLinks().values().stream().skip(elemRand).findFirst().get();
            randomLinks.add(link);
        }
    }

    @Override
    public List<AVVehicle> generateVehicles() {
        long generatedVehicles = 0;
        List<AVVehicle> vehicles = new LinkedList<>();
        while (generatedVehicles < operatorConfig.getGeneratorConfig().getNumberOfVehicles()) {
            ++generatedVehicles;

            /** select one of the 10 random links for placement */
            int elemRand = MatsimRandom.getRandom().nextInt(randomLinks.size());
            Link linkSel = randomLinks.stream().skip(elemRand).findFirst().get();

            LOGGER.info("car placed at link " + linkSel);

            Id<DvrpVehicle> id = AVUtils.createId(operatorConfig.getId(), generatedVehicles);
            AVVehicle vehicle = new AVVehicle(id, linkSel, 0.0, Double.POSITIVE_INFINITY, vehicleType);
            vehicles.add(vehicle);

        }
        return vehicles;
    }

    public static class Factory implements AVGenerator.AVGeneratorFactory {
        @Override
        public AVGenerator createGenerator(OperatorConfig operatorConfig, Network network, VehicleType vehicleType) {
            return new DemoGenerator(operatorConfig, network, vehicleType);
        }
    }
}