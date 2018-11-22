package amod.demo.dispatcher.carpooling;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.core.SharedPartitionedDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractRoboTaxiDestMatcher;
import ch.ethz.idsc.amodeus.dispatcher.util.AbstractVirtualNodeDest;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.DistanceHeuristics;
import ch.ethz.idsc.amodeus.dispatcher.util.EuclideanDistanceFunction;
import ch.ethz.idsc.amodeus.dispatcher.util.GlobalBipartiteMatching;
import ch.ethz.idsc.amodeus.dispatcher.util.RandomVirtualNodeDest;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.traveldata.TravelData;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.config.AVGeneratorConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.router.AVRouter;

public class FlowsOut extends SharedPartitionedDispatcher {

	private final int dispatchPeriod;
	private final Network network;
	private final DistanceHeuristics distanceHeuristics;
	private final Config config;
	private final int timeStep;
	private final int planningHorizon;
	private LinkWait linkWait;

	protected FlowsOut(Config config, //
			AVDispatcherConfig avconfig, //
			AVGeneratorConfig generatorConfig, //
			TravelTime travelTime, //
			AVRouter router, //
			EventsManager eventsManager, //
			Network network, //
			VirtualNetwork<Link> virtualNetwork, //
			AbstractVirtualNodeDest abstractVirtualNodeDest, //
			AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher, //
			TravelData travelData, //
			MatsimAmodeusDatabase db) {
		super(config, avconfig, travelTime, router, eventsManager, virtualNetwork, db);
		this.network = network;
		SafeConfig safeConfig = SafeConfig.wrap(avconfig);
		distanceHeuristics = DistanceHeuristics.valueOf(safeConfig.getString("distanceHeuristics", //
				DistanceHeuristics.EUCLIDEAN.name()).toUpperCase());
		System.out.println("Using DistanceHeuristics: " + distanceHeuristics.name());
		this.config = config;
		this.timeStep = 5;
		// dispatchPeriod = safeConfig.getInteger("dispatchPeriod", timeStep *
		// 60);
		dispatchPeriod = timeStep * 60;
		this.planningHorizon = 10;


	}

	@Override
	protected void redispatch(double now) {

		final long round_now = Math.round(now);

		if (round_now % dispatchPeriod == 0 && round_now >= dispatchPeriod) {

			// travel times
			
//			linkWait = new LinkWait(new HashMap<VirtualNode<Link>, List<Link>>());
//			HashMap<VirtualNode<Link>, List<Link>> linkMap = linkWait.getLinkWait();
//			Pair<List<double[][]>, HashMap<VirtualNode<Link>, List<Link>>> FlowsOutpair = ICRApoolingDispatcherUtils
//					.getFlowsOut(network, virtualNetwork, planningHorizon, timeStep, config, round_now, linkMap);
//			List<double[][]> FlowsOut = FlowsOutpair.getLeft();
//			linkMap = FlowsOutpair.getRight();
//			linkWait.setLinkWait(linkMap);
		    
		    double[] numberRequests = NumberRequestsPopulation.getNumberRequests(network, timeStep, config);

			try {
				// initialize server
				JavaContainerSocket javaContainerSocket = new JavaContainerSocket(
						new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

				{ // add inputs to server
					Container container = new Container("Network");
					
                    container.add((new DoubleArray("NumberRequests", new int[] { numberRequests.length }, numberRequests)));

//					int flowIndex = 0;
//					for (double[][] flows : FlowsOut) {
//						double[] flowsOutAt = new double[flows.length];
//						for (int index = 0; index < flows.length; ++index) {
//							flowsOutAt = flows[index];
//							container.add((new DoubleArray("flowsOut" + flowIndex + 0 + index,
//									new int[] { flows.length }, flowsOutAt)));
//						}
//						flowIndex = flowIndex + 1;
//					}
//
//					// add planning horizon to container
//					double[] PlanningHorizonDouble = new double[] { planningHorizon };
//					container.add((new DoubleArray("PlanningHorizon", new int[] { 1 }, PlanningHorizonDouble)));

					System.out.println("Sending to server");
					javaContainerSocket.writeContainer(container);

				}

				{ // get outputs from server
					System.out.println("Waiting for server");
					Container container = javaContainerSocket.blocking_getContainer();
					// System.out.println("received: " + container);
				}

				javaContainerSocket.close();
				System.out.println("finished");
			} catch (Exception exception) {
				exception.printStackTrace();
				throw new RuntimeException(); // dispatcher will not work if
												// constructor has issues
			}
		}

	}


	public static class Factory implements AVDispatcherFactory {
		@Inject
		@Named(AVModule.AV_MODE)
		private TravelTime travelTime;

		@Inject
		private EventsManager eventsManager;

		@Inject(optional = true)
		private TravelData travelData;

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
			AbstractRoboTaxiDestMatcher abstractVehicleDestMatcher = new GlobalBipartiteMatching(
					EuclideanDistanceFunction.INSTANCE);

			return new FlowsOut(config, avconfig, generatorConfig, travelTime, router, eventsManager,
					network, virtualNetwork, abstractVirtualNodeDest, abstractVehicleDestMatcher, travelData, db);
		}
	}

}