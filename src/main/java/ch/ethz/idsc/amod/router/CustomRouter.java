/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod.router;

import java.io.IOException;
import java.util.concurrent.Future;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

/** This is a nonfunctional sample demonstrating of how to include a custom router
 * to AMoDeus which is not the standard choice of the Paralllel Djikstra router used
 * normally to calculate the path for {@link RoboTaxi} */
/* package */ class CustomRouter implements AmodeusRouter {

    @Override
    public Future<Path> calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
        /** here a path neets to be computed and returned accordign to your custom logic */
        throw new RuntimeException("This CustomRouter is not functional.");
    }

    @Override
    public void close() throws IOException {
        /** here all Threads that were opened during execution should be closed, this function
         * is called within AMoDeus after the simulation has ended. */
        throw new RuntimeException("This CustomRouter is not functional.");
    }

    /** here it is possible to inject objects such as the {@link Network}, see as
     * example the DefaultAVRouter */
    public static class Factory implements AmodeusRouter.Factory {
        @Override
        public AmodeusRouter createRouter(InstanceGetter inject) {
            return new CustomRouter();
        }
    }
}
