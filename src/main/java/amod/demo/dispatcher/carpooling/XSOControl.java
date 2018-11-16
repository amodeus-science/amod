package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.shared.SharedMealType;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class XSOControl {
	private List<List<double[]>> controlLaw;

	public XSOControl(List<List<double[]>> controlLaw) {
		this.controlLaw = controlLaw;
	}

	List<Pair<RoboTaxi, Link>> getXSOCommands(VirtualNetwork<Link> virtualNetwork,
			Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi, List<Link> linkList) throws Exception {

		List<Pair<RoboTaxi, Link>> xSOCommandsList = new ArrayList<>();

		for (VirtualNode<Link> destinationNode : virtualNetwork.getVirtualNodes()) {
			for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {

				List<RoboTaxi> availableCarsNotFiltered = soRoboTaxi.get(fromNode);
				List<RoboTaxi> availableCars = availableCarsNotFiltered.stream()
						.filter(car -> (car.getMenu().getStarterCourse().getMealType() == SharedMealType.DROPOFF
								&& destinationNode.getLinks().contains(car.getCurrentDriveDestination()))
								|| (car.getMenu().getStarterCourse().getMealType() == SharedMealType.REDIRECT
										&& destinationNode.getLinks()
												.contains(car.getMenu().getCourses().get(1).getLink())))
						.collect(Collectors.toList());

				if (availableCars.isEmpty()) {
					continue;
				}

				double[] controlXso = controlLaw.get(destinationNode.getIndex()).get(fromNode.getIndex());

				if (Arrays.stream(controlXso).sum() == 0) {
					continue;
				}

				for (int ixso = 0; ixso < controlXso.length; ixso++) {
					int toNodeRedirect = (int) controlXso[ixso] - 1;
					if (toNodeRedirect < 0) {
						continue;
					}

					if (availableCars.isEmpty()) {
						break;
					}

					RoboTaxi nextRoboTaxi = availableCars.get(0);
					availableCars.remove(nextRoboTaxi);
					soRoboTaxi.get(fromNode).remove(nextRoboTaxi);
					VirtualNode<Link> toVirtualNodeRedirect = virtualNetwork.getVirtualNode(toNodeRedirect);
					Link redirectLink = linkList.get(toVirtualNodeRedirect.getIndex());

					Pair<RoboTaxi, Link> xSOCommands = Pair.of(nextRoboTaxi, redirectLink);
					xSOCommandsList.add(xSOCommands);
					removeXZOCommand(destinationNode, fromNode, ixso);

				}
			}
		}

		return xSOCommandsList;
	}

	List<List<double[]>> getControlLawXSO() {
		return controlLaw;
	}

	void removeXZOCommand(VirtualNode<Link> fromNode, VirtualNode<Link> toNode, int toNodeSecond) {
		controlLaw.get(fromNode.getIndex()).get(toNode.getIndex())[toNodeSecond] = 0;
	}

}
