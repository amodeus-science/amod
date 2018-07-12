package amod.demo.router;

import java.io.IOException;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.av.framework.AVConfigGroup;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.plcpc.DefaultParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.router.AVRouter;
import ch.ethz.matsim.av.router.DefaultAVRouter;

public class IAMoDRouter implements AVRouter {
    final private ParallelLeastCostPathCalculator delegate;

    IAMoDRouter(ParallelLeastCostPathCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Path> calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person,
            Vehicle vehicle) {
        return delegate.calcLeastCostPath(fromNode, toNode, starttime, person, vehicle);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public static class Factory implements AVRouter.Factory {
        @Inject
        AVConfigGroup config;
        @Inject
        @Named(AVModule.AV_MODE)
        TravelTime travelTime;
        @Inject
        @Named(AVModule.AV_MODE)
        Network network;

        @Override
        public AVRouter createRouter() {
            return new IAMoDRouter(DefaultParallelLeastCostPathCalculator.create((int) config.getParallelRouters(),
                    new DijkstraFactory(), network, new FlowDependentTravelDisutility(travelTime), travelTime));
        }
    }
}