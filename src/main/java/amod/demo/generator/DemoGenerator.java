/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.data.AVVehicle;
import ch.ethz.matsim.av.generator.AVGenerator;

/** the initial placment of {@link RoboTaxi} in the {@link Network} is determined with an
 * {@link AVGenerator}. In most cases it is sufficient to use the {@link RandomDensityGenerator}
 * provided in AMoDeus, however, users may wish to have an initial placemnt of {@link RoboTaxi}
 * determined by themselves. This class demonstrates such a placement in which 10 random links are
 * chosen and all vehicles are placed on these random links. */
public class DemoGenerator implements AVGenerator {
    private static final Logger LOGGER = Logger.getLogger(DemoGenerator.class);
    // ---
    private final long numberOfVehicles;
    private final String prefix;
    private final Network network;
    private final Collection<Link> randomLinks = new ArrayList<>();

    private int generatedNumberOfVehicles = 0;

    public DemoGenerator(AVGeneratorConfig config, Network networkIn, Population population) {

        numberOfVehicles = config.getNumberOfVehicles();

        String config_prefix = config.getPrefix();
        prefix = config_prefix == null ? "av_" + config.getParent().getId().toString() + "_" : config_prefix + "_";

        network = Objects.requireNonNull(networkIn);

        /** select 10 random links */
        int bound = network.getLinks().size();
        for (int i = 0; i < 10; ++i) {
            int elemRand = MatsimRandom.getRandom().nextInt(bound);
            Link link = network.getLinks().values().stream().skip(elemRand).findFirst().get();
            randomLinks.add(link);
        }

    }

    /** this function is called to check if an addtional {@link RoboTaxi} can be added. */
    @Override // from Iterator
    public boolean hasNext() {
        return generatedNumberOfVehicles < numberOfVehicles;
    }

    /** This function adds an additional {@link RoboTaxi} */
    @Override // from Iterator
    public AVVehicle next() {
        ++generatedNumberOfVehicles;

        int bound = randomLinks.size();
        int elemRand = MatsimRandom.getRandom().nextInt(bound);
        Link linkSel = randomLinks.stream().skip(elemRand).findFirst().get();

        LOGGER.info("car placed at link " + linkSel);

        Id<Vehicle> id = Id.create("av_" + prefix + String.valueOf(generatedNumberOfVehicles), Vehicle.class);
        // TODO SHARED add capacity attribute here
        return new AVVehicle(id, linkSel, 1.0, 0.0, Double.POSITIVE_INFINITY);
    }

    /** factory which is called to instatiate the DemoGenerator inside the framework */
    public static class Factory implements AVGenerator.AVGeneratorFactory {
        @Inject
        private Population population;
        @Inject
        private Network network;

        @Override
        public AVGenerator createGenerator(AVGeneratorConfig generatorConfig) {
            return new DemoGenerator(generatorConfig, network, population);
        }
    }

}
