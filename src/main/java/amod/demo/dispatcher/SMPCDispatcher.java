package amod.demo.dispatcher;

import java.util.List;
import java.util.Map;

import org.gnu.glpk.GLPKConstants;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.AdaptiveRealTimeRebalancingPolicy;
import ch.ethz.idsc.amodeus.dispatcher.core.PartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.BipartiteMatchingUtils;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.FeasibleRebalanceCreator;
import ch.ethz.idsc.amodeus.dispatcher.util.HungarBiPartVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.LPVehicleRebalancing;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import ch.ethz.idsc.tensor.sca.Sign;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.dispatcher.AVDispatcher.AVDispatcherFactory;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.router.AVRouter;

public class SMPCDispatcher extends PartitionedDispatcher {
    private final int rebalancingPeriod;
    private final int dispatchPeriod;
    private final AbstractVirtualNodeDest virtualNodeDest;
    private final AbstractVehicleDestMatcher vehicleDestMatcher;
    private final int numRobotaxi;
    private int total_rebalanceCount = 0;
    private Tensor printVals = Tensors.empty();
    private final LPVehicleRebalancing lpVehicleRebalancing;
    private final DistanceFunction distanceFunction;
    private final DistanceHeuristics distanceHeuristics;
    private final Network network;

    public SMPCDispatcher( //
            Config config, AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            AVRouter router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        numRobotaxi = (int) generatorConfig.getNumberOfVehicles();
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(avconfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 300);
        this.network = network;
        distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
                DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
        System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
        this.distanceFunction = distanceHeuristics.getDistanceFunction(network);

    }

    @Override
    public void redispatch(double now) {

        // PART I: rebalance all vehicles periodically
        final long round_now = Math.round(now);

        if (round_now % rebalancingPeriod == 0 && round_now > 10) { 
            
            

            
        }

        
        if (round_now % dispatchPeriod == 0) {
            
        }
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s RV=%s H=%s", //
                super.getInfoLine(), //
                total_rebalanceCount, //
                printVals.toString() //
        );
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        @Named(AVModule.AV_MODE)
        private Network network;

        @Inject(optional = true)
        private VirtualNetwork<Link> virtualNetwork;

        @Inject
        private Config config;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());

            return new SMPCDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher);
        }
    }
}
