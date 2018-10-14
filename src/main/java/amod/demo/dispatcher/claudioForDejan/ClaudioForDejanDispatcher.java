/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.dispatcher.claudioForDejan;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import amod.demo.ext.UserReferenceFrames;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.dispatcher.core.PartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlan;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlanEntry;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractRoboTaxiDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.BipartiteMatchingUtils;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.GlobalBipartiteMatching;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVConfig;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import ch.ethz.matsim.av.router.AVRouter;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.geometry.transformations.WGS84toAtlantis;


/*claudio wrapper for dejan*/
public class ClaudioForDejanDispatcher extends PartitionedDispatcher {
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
    private final Coord coordNode = new Coord(-122.4322514,37.78096848);
    private final Coord coordNode1 = new Coord(-122.473764,37.778801);
    private final Coord coordNode2 = new Coord(-122.467423,37.693303);
    private final Coord coordNode3 = new Coord(-122.437587,37.774576);
    private final Coord coordNode4 = new Coord(-122.422480,37.798656);
    private final Coord coordNode5 = new Coord(-122.415639,37.758814);
    private final Coord coordNode6 = new Coord(-122.399455,37.681558);
    private final Coord coordNode7 = new Coord(-122.400386,37.784959);
    private final Coord coordNode8 = new Coord(-122.406597,37.627151);
    private final Coord coordNode9 = new Coord(-122.369284,37.824465);
    private final Coord coordNode10 = new Coord(-122.364220,37.587794);
    private final QuadTree<Link> pendingLinkTree;
    private final double[] networkBounds;
    private final List<Id<Node>> listNodes;
    private final Config config;
    
//    final JavaContainerSocket javaContainerSocket;

    public ClaudioForDejanDispatcher( //
            Config config, AVDispatcherConfig avconfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher, //
            MatsimAmodeusDatabase db) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork, db);
        virtualNodeDest = abstractVirtualNodeDest;
        vehicleDestMatcher = abstractVehicleDestMatcher;
        numRobotaxi = (int) generatorConfig.getNumberOfVehicles();
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingLinkTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        for(Link link: network.getLinks().values()) {
            pendingLinkTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
        }       
//        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(avconfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", 300);
        this.network = network;
        distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
                DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
        System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
        this.distanceFunction = distanceHeuristics.getDistanceFunction(network);
        listNodes = ClaudioForDejanUtils.getNetworkReducedNodeList(network);
        this.config = config;

    }

    @Override
    public void redispatch(double now) {

        // PART I: rebalance all vehicles periodically
        final long round_now = Math.round(now);
        if(round_now==2*60*60) {
            List<double[]> networkIAMoD = ClaudioForDejanUtils.getReducedNetwork(network, listNodes);
            List<double[]> networkDistance = ClaudioForDejanUtils.getReducedNetworkDistance(network, listNodes);
            List<double[]> networkVelocity = ClaudioForDejanUtils.getReducedNetworkVelocityForMatlab(network, listNodes);
            List<double[]> networkLocation = ClaudioForDejanUtils.getReducedNetworkLocationForMatlab(network, listNodes);
            List<double[]> networkCapacity = ClaudioForDejanUtils.getReducedNetworkCapacityForMatlab(network, listNodes);
            List<double[]> networkRequest = ClaudioForDejanUtils.getRequestMatlab(network, ClaudioForDejanUtils.getNetworkNodeList(network), config, round_now);
//            for(Link link: network.getLinks().values()) {
//                Node fromNode = link.getFromNode();
//                Node toNode = link.getToNode();
//                int indexFrom = listNodes.indexOf(fromNode.getId());
//                int indexTo = listNodes.indexOf(toNode.getId());
//                double dist = link.getLength();
//                double[] testDist = networkDistance.get(indexFrom);
//                double[] testNod = networkIAMoD.get(indexFrom);
//                
//            }
            
            try {
                // initialize server
                JavaContainerSocket javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

                { // add inputs to server
                Container container = new Container("Network");
                double[] numberNodes = new double[]{networkIAMoD.size()};
                container.add((new DoubleArray("numberNodes", new int[] { 1 }, numberNodes )));
                
                // add network to container
                int index = 0;
                for(double[] nodeRow: networkIAMoD) {
                    container.add((new DoubleArray("roadGraph" + index, new int[] { nodeRow.length }, nodeRow)));
                    index += 1;
                }
                
             // add distance to container
                int indexdist = 0;
                for(double[] dist: networkDistance) {
                    container.add((new DoubleArray("distance" + indexdist, new int[] { dist.length }, dist)));
                    indexdist += 1;
                }
                
             // add velocity to container
                int indexvel = 0;
                for(double[] nodeRow: networkVelocity) {
                    container.add((new DoubleArray("velocity" + indexvel, new int[] { nodeRow.length }, nodeRow)));
                    indexvel += 1;
                }
                
             // add capacity to container
                int indexcap = 0;
                for(double[] nodeRow: networkCapacity) {
                    container.add((new DoubleArray("capacity" + indexcap, new int[] { nodeRow.length }, nodeRow)));
                    indexcap += 1;
                }
                
                
             // add location to container
                int indexloc = 0;
                for(double[] nodeRow: networkLocation) {
                    container.add((new DoubleArray("location" + indexloc, new int[] { nodeRow.length }, nodeRow)));
                    indexloc += 1;
                }
                
             // add request to container
                int indexreq = 0;
                for(double[] nodeRow: networkRequest) {
                    container.add((new DoubleArray("request" + indexreq, new int[] { nodeRow.length }, nodeRow)));
                    indexreq += 1;
                }
                
                double[] numberRequests = new double[]{networkRequest.size()};
                container.add((new DoubleArray("numberRequests", new int[] { 1 }, numberRequests )));
                
                System.out.println("Sending to server");
                javaContainerSocket.writeContainer(container);
                
                }
                
                { // get outputs from server
                System.out.println("Waiting for server");
                Container container = javaContainerSocket.blocking_getContainer();
                System.out.println("received: " + container);
                                       
                
                }
                
                javaContainerSocket.close();
                System.out.println("finished");
            } catch (Exception exception) {
                exception.printStackTrace();
                throw new RuntimeException(); // dispatcher will not work if
                                              // constructor has issues
            }        
        }
        

        if (round_now % rebalancingPeriod == 0 && round_now > 100000) {
            
//            // available idle vehicles at virtual nodes
//            Map<VirtualNode<Link>,List<RoboTaxi>> idleVehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis(); //TODO is this what you want Dejan?
//            
//            // custumer vehicles with destination virtual nodes
//            Map<VirtualNode<Link>,List<RoboTaxi>> customerVehicles = getVirtualNodeArrivingWithCustomerRoboTaxis();
//            
//            // rebalancing vehicles with destination virtual nodes
//            Map<VirtualNode<Link>, List<RoboTaxi>> rebalanceVehicles = getVirtualNodeRebalancingToRoboTaxis();
//            
//            // travel times
//            Map<VirtualLink<Link>, Double> travelTimes = TravelTimeCalculatorClaudioForDejan.computeTravelTimes(virtualNetwork.getVirtualLinks());
//            
//            // planning horizon for SMPC
//            int PlanningHorizon = 50;
//            
//            // Rebalancing links
//            Coord coord1 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode1);
//            Coord coord2 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode2);
//            Coord coord3 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode3);
//            Coord coord4 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode4);
//            Coord coord5 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode5);
//            Coord coord6 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode6);
//            Coord coord7 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode7);
//            Coord coord8 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode8);
//            Coord coord9 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode9);
//            Coord coord10 = UserReferenceFrames.SANFRANCISCO.coords_fromWGS84().transform(coordNode10);
//            Link link1 = pendingLinkTree.getClosest(coord1.getX(), coord1.getY());
//            Link link2 = pendingLinkTree.getClosest(coord2.getX(), coord2.getY());
//            Link link3 = pendingLinkTree.getClosest(coord3.getX(), coord3.getY());
//            Link link4 = pendingLinkTree.getClosest(coord4.getX(), coord4.getY());
//            Link link5 = pendingLinkTree.getClosest(coord5.getX(), coord5.getY());
//            Link link6 = pendingLinkTree.getClosest(coord6.getX(), coord6.getY());
//            Link link7 = pendingLinkTree.getClosest(coord7.getX(), coord7.getY());
//            Link link8 = pendingLinkTree.getClosest(coord8.getX(), coord8.getY());
//            Link link9 = pendingLinkTree.getClosest(coord9.getX(), coord9.getY());
//            Link link10 = pendingLinkTree.getClosest(coord10.getX(), coord10.getY());
//            Pair<Integer, Link> pair1 = Pair.of(0, link1);
//            Pair<Integer, Link> pair2 = Pair.of(1, link2);
//            Pair<Integer, Link> pair3 = Pair.of(2, link3);
//            Pair<Integer, Link> pair4 = Pair.of(3, link4);
//            Pair<Integer, Link> pair5 = Pair.of(4, link5);
//            Pair<Integer, Link> pair6 = Pair.of(5, link6);
//            Pair<Integer, Link> pair7 = Pair.of(6, link7);
//            Pair<Integer, Link> pair8 = Pair.of(7, link8);
//            Pair<Integer, Link> pair9 = Pair.of(8, link9);
//            Pair<Integer, Link> pair10 = Pair.of(9, link10);
//            List<Pair<Integer, Link>> listpair = new ArrayList<Pair<Integer, Link>>();
//            listpair.add(pair1);
//            listpair.add(pair2);
//            listpair.add(pair3);
//            listpair.add(pair4);
//            listpair.add(pair5);
//            listpair.add(pair6);
//            listpair.add(pair7);
//            listpair.add(pair8);
//            listpair.add(pair9);
//            listpair.add(pair10);
//            // prepare inputs for SMPC in MATLAB
//            double[][] networkSMPC = ClaudioForDejanUtils.getVirtualNetworkForMatlab(virtualNetwork);
//            double [][] travelTimesSMPC = ClaudioForDejanUtils.getTravelTimesForMatlab(virtualNetwork, travelTimes);
//            double[][] availableCarsSMP = ClaudioForDejanUtils.getTotalAvailableCarsForMatlab(round_now, PlanningHorizon, idleVehicles, rebalanceVehicles, customerVehicles);                              
//            
//            try {
//                // initialize server
//                JavaContainerSocket javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));
//
//                { // add inputs to server
//                Container container = ClaudioForDejanUtils.getContainerInit();
//                
//                // add network to container
//                double[] networkNode = new double[networkSMPC.length];
//                for(int index = 0; index<networkSMPC.length; ++index) {
//                    networkNode = networkSMPC[index];
//                    container.add((new DoubleArray("roadGraph" + index, new int[] { networkSMPC.length }, networkNode)));
//                }
//                
//                // add travel times to container
//                double[] travelTimeskNode = new double[travelTimesSMPC.length];
//                for(int index = 0; index<travelTimesSMPC.length; ++index) {
//                    travelTimeskNode = travelTimesSMPC[index];
//                    container.add((new DoubleArray("travelTimes" + index, new int[] { travelTimesSMPC.length }, travelTimeskNode)));
//                }
//                
//                // add available cars to container
//                double[] availableCarsNode = new double[availableCarsSMP.length];
//                int indexCar = 0;
//                for(double[] CarsAtTime: availableCarsSMP) {
//                    indexCar = indexCar + 1;
//                    availableCarsNode = CarsAtTime;
//                    container.add((new DoubleArray("availableCars" + indexCar, new int[] { availableCarsNode.length }, availableCarsNode)));
//                }
//                                
//                // add planning horizon to container
//                double[] PlanningHorizonDouble = new double[] {PlanningHorizon};
//                container.add((new DoubleArray("PlanningHorizon", new int[] { 1 }, PlanningHorizonDouble)));
//                
//             // add planning horizon to container
//                double[] currentTime = new double[] {round_now};
//                container.add((new DoubleArray("currentTime", new int[] { 1 }, currentTime)));
//                
//                System.out.println("Sending to server");
//                javaContainerSocket.writeContainer(container);
//                
//                }
//                
//                { // get outputs from server
//                System.out.println("Waiting for server");
//                Container container = javaContainerSocket.blocking_getContainer();
//                System.out.println("received: " + container);
//                
//                // get control inputs for rebalancing from container
//                List<double[]> ControlLaw = new ArrayList<>();        
//                for(int i=1; i<=container.size(); ++i) {
//                    ControlLaw.add(ClaudioForDejanUtils.getArray(container, "solution"+i));
//                }
//                
//                // apply rebalancing commands
//                CarLinkSelectorClaudioForDejan CarLinkSelect = new CarLinkSelectorClaudioForDejan(idleVehicles);
//                
//                for(int i=0; i<virtualNetwork.getVirtualNodes().size(); ++i) {
//                    VirtualNode<Link> from = virtualNetwork.getVirtualNode(i);
//                    List<Pair<RoboTaxi, Link>> controlPolicy = CarLinkSelect.getRebalanceCommands(from, ControlLaw.get(i), virtualNetwork, listpair);
//                    if(controlPolicy != null) {
//                        for(Pair<RoboTaxi, Link> pair: controlPolicy) {
//                            setRoboTaxiRebalance(pair.getLeft(), pair.getRight());
//                        }
//                        
//                    }
//                     
//                }                                              
//                
//                }
//                
//                javaContainerSocket.close();
//                
//            } catch (Exception exception) {
//                exception.printStackTrace();
//                throw new RuntimeException(); // dispatcher will not work if
//                                              // constructor has issues
//            }        
//                                 
//            System.out.println("Finished rebalancing");
            
                      
        }


        if (round_now % dispatchPeriod == 0) {
            // TODO currently no dispatching done, how would you like to do that?
//            printVals = BipartiteMatchingUtils.executePickup(this, getDivertableRoboTaxis(), getAVRequests(), //
//                    distanceFunction, network, false);

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
        
        @Inject
        private MatsimAmodeusDatabase db;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig, AVRouter router) {
            AVGeneratorConfig generatorConfig = avconfig.getParent().getGeneratorConfig();

            AbstractVirtualNodeDest abstractVirtualNodeDest = new RandomVirtualNodeDest();
            AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(EuclideanDistanceFunction.INSTANCE);

            return new ClaudioForDejanDispatcher(config, avconfig, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork, abstractVirtualNodeDest,
                    abstractVehicleDestMatcher, db);
        }
    }
}
