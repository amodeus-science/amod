/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.core;

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

import ch.ethz.idsc.amodeus.dispatcher.core.RebalancingDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.router.AVRouter;

public class AidoDispatcherHost extends RebalancingDispatcher {
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

    protected AidoDispatcherHost(Network network, Config config, OperatorConfig operatorConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager, //
            StringSocket clientSocket, int numReqTot, //
            MatsimAmodeusDatabase db) {
        super(config, operatorConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db);
        this.db = db;
        this.clientSocket = Objects.requireNonNull(clientSocket);
        this.numReqTot = numReqTot;
        this.fastLinkLookup = new FastLinkLookup(network, db);
        SafeConfig safeConfig = SafeConfig.wrap(operatorConfig.getDispatcherConfig());
        this.dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        aidoReqComp = new AidoRequestCompiler(db);
        aidoRobTaxComp = new AidoRoboTaxiCompiler(db);
    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (getRoboTaxis().size() > 0 && idRoboTaxiMap.isEmpty()) {
            getRoboTaxis().forEach( //
                    roboTaxi -> idRoboTaxiMap.put(db.getVehicleIndex(roboTaxi), roboTaxi));
            aidoScoreCompiler = new AidoScoreCompiler(getRoboTaxis(), numReqTot, db);
        }

        if (round_now % dispatchPeriod == 0) {

            if (Objects.nonNull(aidoScoreCompiler))
                try {
                    getAVRequests().forEach( //
                            avRequest -> idRequestMap.put(db.getRequestIndex(avRequest), avRequest));

                    Tensor status = Tensors.of(RealScalar.of((long) now), //
                            aidoRobTaxComp.compile(getRoboTaxis()), //
                            aidoReqComp.compile(getAVRequests()), //
                            aidoScoreCompiler.compile(round_now, getRoboTaxis(), getAVRequests()));
                    clientSocket.writeln(status);

                    String fromClient = clientSocket.readLine();

                    Tensor commands = Tensors.fromString(fromClient);
                    CommandConsistency.check(commands);

                    Tensor pickups = commands.get(0);
                    for (Tensor pickup : pickups) {
                        RoboTaxi roboTaxi = idRoboTaxiMap.get(pickup.Get(0).number().intValue());
                        AVRequest avRequest = idRequestMap.get(pickup.Get(1).number().intValue());
                        setRoboTaxiPickup(roboTaxi, avRequest);
                    }

                    Tensor rebalances = commands.get(1);
                    for (Tensor rebalance : rebalances) {
                        RoboTaxi roboTaxi = idRoboTaxiMap.get(rebalance.Get(0).number().intValue());
                        Link link = fastLinkLookup.getLinkFromWGS84(TensorCoords.toCoord(rebalance.get(1)));
                        setRoboTaxiRebalance(roboTaxi, link);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
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
        private Config config;

        @Inject
        private StringSocket stringSocket;

        @Inject
        private int numReqTot;

        @Inject
        private MatsimAmodeusDatabase db;

        @Override
        public AVDispatcher createDispatcher(OperatorConfig operatorConfig, AVRouter router, Network network) {
            return new AidoDispatcherHost(network, config, operatorConfig, travelTime, router, eventsManager, //
                    stringSocket, numReqTot, db);
        }
    }

}
