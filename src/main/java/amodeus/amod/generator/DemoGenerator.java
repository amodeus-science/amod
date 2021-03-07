/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.amodeus.components.AmodeusGenerator;
import org.matsim.amodeus.components.generator.AmodeusIdentifiers;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.core.gbl.MatsimRandom;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.generator.RandomDensityGenerator;


/** the initial placement of {@link RoboTaxi} in the {@link Network} is determined
 * with an {@link AmodeusGenerator}. In most cases it is sufficient to use the
 * {@link RandomDensityGenerator} provided in AMoDeus, however, users may wish
 * to have an initial placement of {@link RoboTaxi} determined by themselves.
 * This class demonstrates such a placement in which 10 random links are chosen
 * and all vehicles are placed on these random links. */
public class DemoGenerator implements AmodeusGenerator {
    private static final Logger LOGGER = Logger.getLogger(DemoGenerator.class);
    // ---
    private final int capacity;
    private final AmodeusModeConfig operatorConfig;
    private final Collection<Link> randomLinks = new ArrayList<>();

    public DemoGenerator(AmodeusModeConfig operatorConfig, Network network, int capacity) {
        this.operatorConfig = operatorConfig;
        this.capacity = capacity;

        /** determine 10 random links in the network */
        int bound = network.getLinks().size();
        for (int i = 0; i < 10; ++i) {
            int elemRand = MatsimRandom.getRandom().nextInt(bound);
            Link link = network.getLinks().values().stream().skip(elemRand).findFirst().get();
            randomLinks.add(link);
        }
    }

    @Override
    public List<DvrpVehicleSpecification> generateVehicles() {
        long generatedVehicles = 0;
        List<DvrpVehicleSpecification> vehicles = new LinkedList<>();
        while (generatedVehicles < operatorConfig.getGeneratorConfig().getNumberOfVehicles()) {
            ++generatedVehicles;

            /** select one of the 10 random links for placement */
            int elemRand = MatsimRandom.getRandom().nextInt(randomLinks.size());
            Link linkSel = randomLinks.stream().skip(elemRand).findFirst().get();

            LOGGER.info("car placed at link " + linkSel);

            Id<DvrpVehicle> id = AmodeusIdentifiers.createVehicleId(operatorConfig.getMode(), generatedVehicles);

            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder() //
                    .id(id) //
                    .serviceBeginTime(0.0) //
                    .serviceEndTime(Double.POSITIVE_INFINITY) //
                    .capacity(capacity) //
                    .startLinkId(linkSel.getId()) //
                    .build());
        }
        return vehicles;
    }

    public static class Factory implements AmodeusGenerator.AVGeneratorFactory {
        @Override
        public AmodeusGenerator createGenerator(InstanceGetter inject) {
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            Network network = inject.getModal(Network.class);
            int capacity = operatorConfig.getGeneratorConfig().getCapacity();

            return new DemoGenerator(operatorConfig, network, capacity);
        }
    }
}