/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.router;

import java.io.IOException;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.router.AVRouter;

/** This is a nonfunctional sample demonstrating of how to include a custom router
 * to AMoDeus which is not the standard choice of the Paralllel Djikstra router used
 * normally to calculate the path for {@link RoboTaxi} */
public class CustomRouter implements AVRouter {

    @Override
    public Future<Path> calcLeastCostPath(Node fromNode, Node toNode, double starttime, //
            Person person, Vehicle vehicle) {
        /** here a path neets to be computed and returned accordign to your custom logic */
        System.err.println("This CustomRouter is not functional.");
        GlobalAssert.that(false);
        return null;
    }

    @Override
    public void close() throws IOException {
        /** here all Threads that were opened during execution should be closed, this function
         * is called within AMoDeus after the simulation has ended. */
        System.err.println("This CustomRouter is not functional.");
        GlobalAssert.that(false);
    }

    /** here it is possible to inject objects such as the {@link Network}, see as
     * example the DefaultAVRouter */
    public static class Factory implements AVRouter.Factory {
        @Override
        public AVRouter createRouter() {
            return new CustomRouter();
        }
    }

}
