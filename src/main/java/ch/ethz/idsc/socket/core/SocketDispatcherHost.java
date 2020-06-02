/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import amodeus.amodeus.dispatcher.core.RebalancingDispatcher;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.net.FastLinkLookup;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.util.matsim.SafeConfig;
import amodeus.amodeus.util.net.StringSocket;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

// TODO refactor and shorten @clruch
public class SocketDispatcherHost extends RebalancingDispatcher {
    private final MatsimAmodeusDatabase db;

    private final Map<Integer, RoboTaxi> idRoboTaxiMap = new HashMap<>();
    private final Map<Integer, PassengerRequest> idRequestMap = new HashMap<>();
    private final FastLinkLookup fastLinkLookup;
    private final StringSocket clientSocket;
    private final int numReqTot;
    private final int dispatchPeriod;
    private final SocketRequestCompiler socketReqComp;
    private final SocketRoboTaxiCompiler socketRobTaxComp;
    // ---
    private SocketScoreCompiler socketScoreCompiler;

    protected SocketDispatcherHost(Network network, Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime,
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
        socketReqComp = new SocketRequestCompiler(db);
        socketRobTaxComp = new SocketRoboTaxiCompiler(db);
    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (getRoboTaxis().size() > 0 && idRoboTaxiMap.isEmpty()) {
            getRoboTaxis().forEach( //
                    roboTaxi -> idRoboTaxiMap.put(roboTaxi.getId().index(), roboTaxi));
            socketScoreCompiler = new SocketScoreCompiler(getRoboTaxis(), numReqTot, db);
        }

        if (round_now % dispatchPeriod == 0) {

            if (Objects.nonNull(socketScoreCompiler))
                try {
                    getPassengerRequests().forEach( //
                            avRequest -> idRequestMap.put(avRequest.getId().index(), avRequest));

                    Tensor status = Tensors.of(RealScalar.of((long) now), //
                            socketRobTaxComp.compile(getRoboTaxis()), //
                            socketReqComp.compile(getPassengerRequests()), //
                            socketScoreCompiler.compile(round_now, getRoboTaxis(), getPassengerRequests()));
                    clientSocket.writeln(status);

                    String fromClient = clientSocket.readLine();

                    Tensor commands = Tensors.fromString(fromClient);
                    CommandConsistency.check(commands);

                    Tensor pickups = commands.get(0);
                    for (Tensor pickup : pickups) {
                        RoboTaxi roboTaxi = idRoboTaxiMap.get(pickup.Get(0).number().intValue());
                        PassengerRequest avRequest = idRequestMap.get(pickup.Get(1).number().intValue());
                        setRoboTaxiPickup(roboTaxi, avRequest);
                    }

                    Tensor rebalances = commands.get(1);
                    for (Tensor rebalance : rebalances) {
                        RoboTaxi roboTaxi = idRoboTaxiMap.get(rebalance.Get(0).number().intValue());
                        Link link = fastLinkLookup.linkFromWGS84(rebalance.get(1));
                        setRoboTaxiRebalance(roboTaxi, link);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
        }
    }

    public static class Factory implements AVDispatcherFactory {
        @Override
        public AmodeusDispatcher createDispatcher(InstanceGetter inject) {
            Config config = inject.get(Config.class);
            MatsimAmodeusDatabase db = inject.get(MatsimAmodeusDatabase.class);
            EventsManager eventsManager = inject.get(EventsManager.class);

            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            Network network = inject.getModal(Network.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);

            // TODO: Probably worth configuring this in some other way (not binding String and int)
            int numReqTot = inject.get(int.class);
            StringSocket stringSocket = inject.get(StringSocket.class);

            return new SocketDispatcherHost(network, config, operatorConfig, travelTime, router, eventsManager, //
                    stringSocket, numReqTot, db);
        }
    }
}
