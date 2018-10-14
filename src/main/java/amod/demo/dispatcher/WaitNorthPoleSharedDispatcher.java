package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.SharedRebalancingDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.shared.NorthPoleSharedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedCourse;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractRoboTaxiDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.GlobalBipartiteMatching;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.dispatcher.AVDispatcher.AVDispatcherFactory;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.router.AVRouter;

public class WaitNorthPoleSharedDispatcher extends SharedRebalancingDispatcher {

    private final int dispatchPeriod;
    private final int rebalancePeriod;
    private final List<Link> links;
    private final Random randGen = new Random(1234);
    private final Link cityNorthPole;
    private final List<Link> equatorLinks;

    protected WaitNorthPoleSharedDispatcher(Network network, //
            Config config, AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, AVRouter router, EventsManager eventsManager, MatsimAmodeusDatabase db) {
        super(config, avDispatcherConfig, travelTime, router, eventsManager, db);
        this.cityNorthPole = getNorthPole(network);
        this.equatorLinks = getEquator(network);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancePeriod = safeConfig.getInteger("rebalancePeriod", 1800);
        links = new ArrayList<>(network.getLinks().values());
        Collections.shuffle(links, randGen);
    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0 && round_now==12000) {
            /** assignment of {@link RoboTaxi}s */
                if (getUnassignedAVRequests().size() >= 1 && !getDivertableUnassignedRoboTaxis().isEmpty()) {

                    RoboTaxi sharedRoboTaxi = getDivertableUnassignedRoboTaxis().iterator().next();
                    
                    /** select 4 requests */
                    AVRequest firstRequest = getUnassignedAVRequests().get(0);


                    /** add pickup for request 1 */
                    addSharedRoboTaxiPickup(sharedRoboTaxi, firstRequest);


                    /** add a waiting task (to the north pole) and move to prev */
                    Link waitingtLink = cityNorthPole;
                    SharedCourse waitingCourse = SharedCourse.waitingCourse(waitingtLink, //
                            Double.toString(now) + sharedRoboTaxi.getId().toString());
                    addSharedRoboTaxiWaiting(sharedRoboTaxi, waitingCourse);
                    sharedRoboTaxi.getMenu().moveAVCourseToPrev(waitingCourse);

                    /** check consistency and end */
                    sharedRoboTaxi.checkMenuConsistency();
                
                }
            
        }

        /** dispatching of available {@link RoboTaxi}s to the equator */
        if (round_now % rebalancePeriod == 0) {
            /** relocation of empty {@link RoboTaxi}s to a random link on the equator */
            for (RoboTaxi roboTaxi : getDivertableUnassignedRoboTaxis()) {
                Link rebalanceLink = equatorLinks.get(randGen.nextInt(equatorLinks.size()));
                setRoboTaxiRebalance(roboTaxi, rebalanceLink);
            }
        }
    }

    /** @param network
     * @return northern most {@link Link} in the {@link Network} */
    private static Link getNorthPole(Network network) {
        NavigableMap<Double, Link> links = new TreeMap<>();
        network.getLinks().values().stream().forEach(l -> {
            links.put(l.getCoord().getY(), l);
        });
        return links.lastEntry().getValue();
    }

    /** @param network
     * @return all {@link Link}s crossing the equator of the city {@link Network} , starting
     *         with links on the equator, if no links found, the search radius is increased by 1 m */
    private static List<Link> getEquator(Network network) {
        NavigableMap<Double, Link> links = new TreeMap<>();
        network.getLinks().values().stream().forEach(l -> {
            links.put(l.getCoord().getY(), l);
        });
        double northX = links.lastEntry().getValue().getCoord().getY();
        double southX = links.firstEntry().getValue().getCoord().getY();
        double equator = southX + (northX - southX) / 2;

        List<Link> equatorLinks = new ArrayList<>();
        double margin = 0.0;
        while (equatorLinks.size() < 1) {
            for (Link l : network.getLinks().values()) {
                boolean crossEq1 = l.getFromNode().getCoord().getY() - margin <= //
                equator && l.getToNode().getCoord().getY() + margin >= equator;
                boolean crossEq2 = l.getFromNode().getCoord().getY() + margin >= //
                equator && l.getToNode().getCoord().getY() - margin <= equator;
                if (crossEq1 || crossEq2)
                    equatorLinks.add(l);
            }
            margin += 1.0;
        }
        GlobalAssert.that(equatorLinks.size() > 0);
        return equatorLinks;
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
        private MatsimAmodeusDatabase db;

        @Override

        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            @SuppressWarnings("unused")
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            @SuppressWarnings("unused")
            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            @SuppressWarnings("unused")
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(EuclideanDistanceFunction.INSTANCE);

            return new WaitNorthPoleSharedDispatcher(network, config, avconfig, travelTime, router, eventsManager, db);
        }
    }
}

