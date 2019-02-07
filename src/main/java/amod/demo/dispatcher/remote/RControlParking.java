package amod.demo.dispatcher.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;

public class RControlParking {
	private Tensor controlLaw;

	public RControlParking(Tensor controlLaw) {
		this.controlLaw = controlLaw;
	}

	public List<Pair<RoboTaxi, Link>> getRebalanceCommands(Map<VirtualNode<Link>, List<RoboTaxi>> availableVehicles,
			VirtualNetwork<Link> virtualNetwork, List<Link> linkList, Collection<RoboTaxi> emptyDrivingVehicles,
			int maxDrivingEmptyCars, boolean firstRebalance, Map<VirtualNode<Link>, List<RoboTaxi>> waitVehicles)
			throws Exception {

		Collection<VirtualNode<Link>> vNodes = virtualNetwork.getVirtualNodes();
		List<Pair<RoboTaxi, Link>> rebalanceCommandsList = new ArrayList<>();

		for (VirtualNode<Link> fromNode : vNodes) {
			for (VirtualNode<Link> toNode : vNodes) {
				List<RoboTaxi> avTaxis = availableVehicles.get(fromNode);
				List<RoboTaxi> waitavTaxis = waitVehicles.get(fromNode);
				double controlInput = controlLaw.Get(fromNode.getIndex(), toNode.getIndex()).number().doubleValue();
				int numberAssignedCars = 0;
				boolean rebalanceFlag = false;
				List<RoboTaxi> rebalancingParkingCars = new ArrayList<RoboTaxi>();

				if (controlInput == 0) {
					continue;
				}

				if (avTaxis.isEmpty() && waitavTaxis.isEmpty()) {
					continue;
				}

				int iteration = 0;

				for (int i = 1; i <= controlInput; i++) {
					if (avTaxis.isEmpty() && waitavTaxis.isEmpty()) {
						break;
					}

					rebalancingParkingCars = avTaxis.stream().filter(
							car -> (!car.getUnmodifiableViewOfCourses().isEmpty() && car.getUnmodifiableViewOfCourses()
									.get(0).getMealType().equals(SharedMealType.REDIRECT))
									|| (!car.getUnmodifiableViewOfCourses().isEmpty()
											&& car.getUnmodifiableViewOfCourses().get(0).getMealType()
													.equals(SharedMealType.PARK)))
							.collect(Collectors.toList());

					if (emptyDrivingVehicles.size() + numberAssignedCars >= maxDrivingEmptyCars) {
						if (rebalancingParkingCars.isEmpty()) {
							break;
						}
						rebalanceFlag = true;
					}

					RoboTaxi nextRoboTaxi = null;

					if (rebalanceFlag) {
						nextRoboTaxi = rebalancingParkingCars.get(0);
						rebalancingParkingCars.remove(nextRoboTaxi);
						avTaxis.remove(nextRoboTaxi);
						availableVehicles.get(fromNode).remove(nextRoboTaxi);
					} else {
						if (!waitavTaxis.isEmpty()) {
							nextRoboTaxi = waitavTaxis.get(0);
							waitavTaxis.remove(nextRoboTaxi);
							waitVehicles.get(fromNode).remove(nextRoboTaxi);
							numberAssignedCars = numberAssignedCars + 1;
						} else if (!rebalancingParkingCars.isEmpty()) {
							nextRoboTaxi = rebalancingParkingCars.get(0);
							rebalancingParkingCars.remove(nextRoboTaxi);
							avTaxis.remove(nextRoboTaxi);
							availableVehicles.get(fromNode).remove(nextRoboTaxi);
						} else {
							nextRoboTaxi = avTaxis.get(0);
							avTaxis.remove(nextRoboTaxi);
							availableVehicles.get(fromNode).remove(nextRoboTaxi);
							numberAssignedCars = numberAssignedCars + 1;
						}

					}

					GlobalAssert.that(nextRoboTaxi != null);

					Link rebalanceLink = linkList.get(toNode.getIndex());

					Pair<RoboTaxi, Link> xZOCommands = Pair.of(nextRoboTaxi, rebalanceLink);
					rebalanceCommandsList.add(xZOCommands);

					iteration = i;

				}

				RealScalar newControlLaw = DoubleScalar.of(controlInput - iteration);

				controlLaw.set(newControlLaw, fromNode.getIndex(), toNode.getIndex());
			}

		}

		return rebalanceCommandsList;

	}

	public Tensor getControlLawRebalance() {
		return controlLaw;
	}

}
