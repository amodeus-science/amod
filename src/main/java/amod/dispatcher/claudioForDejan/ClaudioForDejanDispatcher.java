/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.dispatcher.claudioForDejan;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.core.PartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.HungarBiPartVehicleDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.LPVehicleRebalancing;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;


/*claudio wrapper for dejan*/
public class ClaudioForDejanDispatcher extends PartitionedDispatcher {
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
    
    final JavaContainerSocket javaContainerSocket;

    public ClaudioForDejanDispatcher( //
            Config config, AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        numRobotaxi = (int) generatorConfig.getNumberOfVehicles();
        try {
            javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

            Container container = ClaudioForDejanUtils.getContainerInit();
            javaContainerSocket.writeContainer(container);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(); // dispatcher will not work if
                                          // constructor has issues
        }        
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
            
            // available vehicles at virtual nodes
            Map<VirtualNode<Link>,List<RoboTaxi>> availableVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis(); //TODO is this what you want Dejan?
                        
            
            // travel times // TODO maybe take out of loop if it does not change in your implmenentation?
            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorClaudioForDejan.computeTravelTimes(virtualNetwork.getVirtualLinks());

            
            // Dejan's rebalancing commands
            Map<VirtualLink<Link>,Integer> rebalanceVehicles = new HashMap<>(); 
            //TODO fill me proplerly
            rebalanceVehicles.put(virtualNetwork.getVirtualLinks().iterator().next(), 1);
            
            // TODO send proper stuff
            javaContainerSocket.writeContainer(ClaudioForDejanUtils.getContainerInit());

            // TODO here your code in the other tool should run
            
            Container container = javaContainerSocket.blocking_getContainer();
            System.out.println("received: " + container);
            
            
            // implement rebalancing and dispatching
            
            CarLinkSelectorClaudioForDejan carLS = new CarLinkSelectorClaudioForDejan(availableVehicles);
            
            for(Entry<VirtualLink<Link>, Integer> entry: rebalanceVehicles.entrySet()){
                

                
                Map<RoboTaxi, Link> rebalanceCommands = carLS.getRebalanceCommands(entry.getKey(), entry.getValue());

                for(Entry<RoboTaxi, Link> rebEntry : rebalanceCommands.entrySet()){
                    setRoboTaxiRebalance(rebEntry.getKey(), rebEntry.getValue());
                }
                
                
            }
            
            
        }


        if (round_now % dispatchPeriod == 0) {
            
            // TODO currently no dispatching done, how would you like to do that?

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
        private ParallelLeastCostPathCalculator router;

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
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig) {
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());

            return new ClaudioForDejanDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher);
        }
    }
}
