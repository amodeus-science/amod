/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.amodeus.dispatcher.core.DispatcherUtils;
import ch.ethz.idsc.amodeus.dispatcher.core.RebalancingDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.util.DrivebyRequestStopper;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.util.matsim.SafeConfig;

/** Dispatcher sends vehicles to all links in the network and lets them pickup
 * any customers which are waiting along the road. */
public class DemoDispatcher extends RebalancingDispatcher {
    private final List<Link> links;
    private final double rebPos = 0.99;
    private final Random randGen = new Random(1234);
    private final int rebalancingPeriod;
    private int total_abortTrip = 0;

    private DemoDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime, //
            AmodeusRouter router, EventsManager eventsManager, Network network, //
            MatsimAmodeusDatabase db) {
        super(config, operatorConfig, travelTime, router, eventsManager, db);
        links = new ArrayList<>(network.getLinks().values());
        SafeConfig safeConfig = SafeConfig.wrap(operatorConfig.getDispatcherConfig());
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 120);
    }

    @Override
    public void redispatch(double now) {

        /** stop all vehicles which are driving by an open request */
        Map<RoboTaxi, PassengerRequest> stopDrivingBy = DrivebyRequestStopper //
                .stopDrivingBy(DispatcherUtils.getPassengerRequestsAtLinks(getPassengerRequests()), //
                        getDivertableRoboTaxis(), this::setRoboTaxiPickup);
        total_abortTrip += stopDrivingBy.size();

        /** send vehicles to travel around the city to random links (random loitering) */
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0 && 0 < getPassengerRequests().size()) {
            for (RoboTaxi roboTaxi : getDivertableRoboTaxis()) {
                if (rebPos > randGen.nextDouble()) {
                    setRoboTaxiRebalance(roboTaxi, pollNextDestination());
                }
            }
        }
    }

    private Link pollNextDestination() {
        Link link = links.get(randGen.nextInt(links.size()));
        return link;
    }

    @Override
    protected String getInfoLine() {
        return String.format("%s AT=%5d", super.getInfoLine(), total_abortTrip);
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

            return new DemoDispatcher(config, operatorConfig, travelTime, router, eventsManager, network, db);
        }
    }

}