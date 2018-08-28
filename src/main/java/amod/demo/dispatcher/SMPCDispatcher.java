package amod.demo.dispatcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gnu.glpk.GLPKConstants;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import amod.demo.dispatcher.claudioForDejan.ClaudioForDejanUtils;
import amod.demo.ext.UserReferenceFrames;
import ch.ethz.idsc.amodeus.dispatcher.AdaptiveRealTimeRebalancingPolicy;
import ch.ethz.idsc.amodeus.dispatcher.core.PartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractRoboTaxiDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.BipartiteMatchingUtils;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.FeasibleRebalanceCreator;
import ch.ethz.idsc.amodeus.dispatcher.util.GlobalBipartiteMatching;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.prep.NetworkCreatorUtils;
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
    private final AbstractRoboTaxiDestMatcher vehicleDestMatcher;
    private final int numRobotaxi;
    private int total_rebalanceCount = 0;
    private Tensor printVals = Tensors.empty();
//    private final LPVehicleRebalancing lpVehicleRebalancing;
    private final DistanceFunction distanceFunction;
    private final DistanceHeuristics distanceHeuristics;
    private final Network network;
    private final Config config;
    private static final Logger logger = Logger.getLogger(SMPCDispatcher.class);

    public SMPCDispatcher( //
            Config config, AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            AVRouter router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        numRobotaxi = (int) generatorConfig.getNumberOfVehicles();
//        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(avconfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 300);
        this.network = network;
        distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
                DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
        System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
        this.distanceFunction = distanceHeuristics.getDistanceFunction(network);
        this.config = config;

    }

    @Override
    public void redispatch(double now) {

        // PART I: rebalance all vehicles periodically
        final long round_now = Math.round(now);
        
        if (round_now % dispatchPeriod == 0) {
//          printVals = BipartiteMatchingUtils.executePickup(this, getDivertableRoboTaxis(), getAVRequests(), //
//          distanceFunction, network, false);
            List<Id<Node>> listNodesInter = ClaudioForDejanUtils.getNetworkReducedNodeList(network);
            List<double[]> networkIAMoD = ClaudioForDejanUtils.getReducedNetwork(network, listNodesInter);
            Node nodetest = network.getNodes().get(listNodesInter.get(60));
            Coord coordtest = nodetest.getCoord();
            Coord coordNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordtest);
            System.out.println(coordNodeWGS84.getY() + "," + coordNodeWGS84.getX());
            for(int i=0; i<networkIAMoD.get(60).length; i++) {
                double nodeId = networkIAMoD.get(60)[i];
                Id<Node> nodeToID = listNodesInter.get((int) nodeId);
                Node nodeTo = network.getNodes().get(nodeToID);
                Coord coordTo = nodeTo.getCoord();
                Coord coordToWGS = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordTo);
                System.out.println(coordToWGS.getY() + "," + coordToWGS.getX());
            }
            
//            Link link = network.getLinks().values().iterator().next();
//            Coord fromNode = link.getFromNode().getCoord();
//            Coord coordfromNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(fromNode);
//            Coord toNode = link.getToNode().getCoord();
//            Coord coordtoNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(toNode);
//            System.out.println(coordfromNodeWGS84.getY() + "," + coordfromNodeWGS84.getX());
//            System.out.println(coordtoNodeWGS84.getY() + "," + coordtoNodeWGS84.getX());
            List<double[]> networkIAMoDDistance = ClaudioForDejanUtils.getReducedNetworkDistance(network, listNodesInter);
            for(int i=0; i<networkIAMoDDistance.get(60).length; i++) {
                double nodeId = networkIAMoDDistance.get(60)[i];
                System.out.println(nodeId);
            }
            List<double[]> networkIAMoDCapacity = ClaudioForDejanUtils.getNetworkCapacityForMatlab(network, listNodesInter);
            List<double[]> networkIAMoDVelocity = ClaudioForDejanUtils.getNetworkVelocityForMatlab(network, listNodesInter);
            
            
         System.out.println("finished");
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
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(EuclideanDistanceFunction.INSTANCE);

            return new SMPCDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher);
        }
    }
}
