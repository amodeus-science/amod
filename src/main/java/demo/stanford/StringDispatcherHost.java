/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package demo.stanford;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import amod.aido.core.AidoRequestCompiler;
import amod.aido.core.AidoRoboTaxiCompiler;
import amod.aido.core.AidoScoreCompiler;
import amod.aido.core.CommandConsistency;
import ch.ethz.idsc.amodeus.dispatcher.core.DispatcherConfig;
import ch.ethz.idsc.amodeus.dispatcher.core.RebalancingDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.util.BipartiteMatchingUtils;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.router.AVRouter;

/** Adapted version of the {@link AidoDispatcher} host that does global bipartite matching and
 * leaves the rebalancing command to the external code communicating through the string based
 * interface.
 * 
 * @author clruch */
public class StringDispatcherHost extends RebalancingDispatcher {
    private final MatsimAmodeusDatabase db;

    private final Map<Integer, RoboTaxi> idRoboTaxiMap = new HashMap<>();
    private final Map<Integer, AVRequest> idRequestMap = new HashMap<>();
    private final FastLinkLookup fastLinkLookup;
    private final StringSocket clientSocket;
    private final int numReqTot;
    private final int dispatchPeriod;
    private final AidoRequestCompiler aidoReqComp;
    private final AidoRoboTaxiCompiler aidoRobTaxComp;
    // ---
    private AidoScoreCompiler aidoScoreCompiler;
    // -- 
    private final Network network;
    private final DistanceFunction distanceFunction;
    private final BipartiteMatchingUtils bipartiteMatchingUtils;


    protected StringDispatcherHost(Network network, Config config, AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager, //
            StringSocket clientSocket, int numReqTot, //
            MatsimAmodeusDatabase db) {
        super(config, avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db);
        this.db = db;
        this.clientSocket = Objects.requireNonNull(clientSocket);
        this.numReqTot = numReqTot;
        this.network = network;
        fastLinkLookup = new FastLinkLookup(network, db);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        aidoReqComp = new AidoRequestCompiler(db);
        aidoRobTaxComp = new AidoRoboTaxiCompiler(db);
        bipartiteMatchingUtils = new BipartiteMatchingUtils(network);
        DispatcherConfig dispatcherConfig = DispatcherConfig.wrap(avDispatcherConfig);
        DistanceHeuristics distanceHeuristics = //
                dispatcherConfig.getDistanceHeuristics(DistanceHeuristics.EUCLIDEAN);
        distanceFunction = distanceHeuristics.getDistanceFunction(network);
    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        /** once {@link RoboTaxi}s become visible, initialize the map with IDs */
        if (getRoboTaxis().size() > 0 && idRoboTaxiMap.isEmpty()) {
            getRoboTaxis().forEach(//
                    s -> idRoboTaxiMap.put(db.getVehicleIndex(s), s));
            aidoScoreCompiler = new AidoScoreCompiler(getRoboTaxis(), numReqTot, db);
        }

        /** time step in fleet operational control. */
        if (round_now % dispatchPeriod == 0) {

            /** bipartite matching is conducted to assign available {@link RoboTaxi}s to
             * open {@link AVRequest}s. */
            bipartiteMatchingUtils.executePickup(this, getDivertableRoboTaxis(), //
                    getAVRequests(), distanceFunction, network);

            if (Objects.nonNull(aidoScoreCompiler))
                try {
                    getAVRequests().forEach(//
                            r -> idRequestMap.put(db.getRequestIndex(r), r));

                    Tensor status = Tensors.of(RealScalar.of((long) now), //
                            aidoRobTaxComp.compile(getRoboTaxis()), //
                            aidoReqComp.compile(getAVRequests()), //
                            aidoScoreCompiler.compile(round_now, getRoboTaxis(), getAVRequests()));
                    clientSocket.writeln(status);

                    String fromClient = null;

                    fromClient = clientSocket.readLine();

                    Tensor commands = Tensors.fromString(fromClient);
                    CommandConsistency.check(commands);

                    Tensor pickups = commands.get(0);
                    /** pickups are simply ignored as they are done via bipartite matching */
                    for (Tensor pickup : pickups) {
                        // -- deliberately empty, for execution of demands, see class AidoDispatcherHost
                    }

                    Tensor rebalances = commands.get(1);
                    for (Tensor rebalance : rebalances) {
                        RoboTaxi roboTaxi = idRoboTaxiMap.get(rebalance.Get(0).number().intValue());
                        Link link = fastLinkLookup.getLinkFromWGS84(TensorCoords.toCoord(rebalance.get(1)));
                        setRoboTaxiRebalance(roboTaxi, link);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
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

        @Inject
        private Config config;

        @Inject
        private StringSocket stringSocket;

        @Inject
        private int numReqTot;

        @Inject
        private MatsimAmodeusDatabase db;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            return new StringDispatcherHost(network, config, avconfig, travelTime, router, eventsManager, //
                    stringSocket, numReqTot, db);
        }
    }

}
