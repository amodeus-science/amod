//package amod.demo.dispatcher.carpooling;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.apache.commons.lang3.tuple.Triple;
//import org.matsim.api.core.v01.network.Link;
//
//import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
//import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
//import ch.ethz.matsim.av.passenger.AVRequest;
//
//public class PSOControl {
//	private List<List<double[]>> controlLaw;
//	private HashMap<VirtualNode<Link>, List<Link>> linkMap;
//
//	public PSOControl(List<List<double[]>> controlLaw, HashMap<VirtualNode<Link>, List<Link>> linkMap) {
//		this.controlLaw = controlLaw;
//		this.linkMap = linkMap;
//	}
//
//	List<Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>>> getPSOCommands(VirtualNetwork<Link> virtualNetwork,
//			Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi,
//			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
//			Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests) throws Exception {
//
//		List<Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>>> pSOCommandsList = new ArrayList<>();
//
//		for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
//			List<RoboTaxi> availableCarsFrom = soRoboTaxi.get(fromNode);
//			if (availableCarsFrom.isEmpty()) {
//				continue;
//			}
//			List<AVRequest> fromRequests = virtualNodeAVFromRequests.get(fromNode);
//
//			for (VirtualNode<Link> toNodeFirst : virtualNetwork.getVirtualNodes()) {
//
//				List<RoboTaxi> availableCars = availableCarsFrom.stream()
//						.filter(car -> (car.getMenu().getCourses().size() == 1
//								&& toNodeFirst.getLinks().contains(car.getMenu().getStarterCourse().getLink()))
//								|| (car.getMenu().getCourses().size() == 2 && toNodeFirst.getLinks()
//										.contains(car.getMenu().getCourses().get(1).getLink())))
//						.collect(Collectors.toList());
//
//				if (availableCars.isEmpty()) {
//					continue;
//				}
//
//				double[] controlPso = controlLaw.get(fromNode.getIndex()).get(toNodeFirst.getIndex());
//
//				if (Arrays.stream(controlPso).sum() == 0) {
//					continue;
//				}
//
//				for (int ipso = 0; ipso < controlPso.length; ipso++) {
//					int toNodeSecondIndex = (int) controlPso[ipso] - 1;
//					if (toNodeSecondIndex < 0) {
//						continue;
//					}
//
//					if (availableCars.isEmpty()) {
//						break;
//					}
//
//					List<AVRequest> toRequestSecondUnfilterd = virtualNodeAVToRequests
//							.get(virtualNetwork.getVirtualNode(toNodeSecondIndex));
//					List<AVRequest> toRequestSecond = fromRequests.stream()
//							.filter(req -> toRequestSecondUnfilterd.contains(req)).collect(Collectors.toList());
//					if (!toRequestSecond.isEmpty()) {
//						AVRequest avRequestSecond = toRequestSecond.get(0);
//						toRequestSecond.remove(avRequestSecond);
//						toRequestSecondUnfilterd.remove(avRequestSecond);
//						virtualNodeAVFromRequests.get(fromNode).remove(avRequestSecond);
//						virtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(toNodeSecondIndex))
//								.remove(avRequestSecond);
//
//						RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestSecond,
//								availableCars);
//						availableCars.remove(closestRoboTaxi);
//						availableCarsFrom.remove(closestRoboTaxi);
//						soRoboTaxi.get(fromNode).remove(closestRoboTaxi);
//
//						Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>> pSOCommands = Triple
//								.of(closestRoboTaxi, avRequestSecond, null);
//
//						pSOCommandsList.add(pSOCommands);
//
//						removePSOCommand(fromNode, toNodeFirst, ipso);
//					} else if (toRequestSecond.isEmpty()) {
//						AVRequest avRequestSecond = null;
//
//						List<Link> linkSet = linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex));
//						List<Link> fromNodeLinkList = linkSet.stream()
//								.filter(link -> fromNode.getLinks().contains(link)).collect(Collectors.toList());
//
//						Link waitingLink = null;
//						RoboTaxi closestRoboTaxi = null;
//						if (!fromNodeLinkList.isEmpty()) {
//							waitingLink = fromNodeLinkList.get(0);
//							linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex)).remove(waitingLink);
//							closestRoboTaxi = StaticHelperCarPooling.findClostestVehicleToLink(waitingLink,
//									availableCars);
//						} else {
//							closestRoboTaxi = availableCars.get(0);
//							waitingLink = closestRoboTaxi.getDivertableLocation();
//						}
//
//						if (closestRoboTaxi == null) {
//							System.out.println("NULL");
//						}
//
//						availableCars.remove(closestRoboTaxi);
//						availableCarsFrom.remove(closestRoboTaxi);
//						soRoboTaxi.get(fromNode).remove(closestRoboTaxi);
//
//						Pair<Link, VirtualNode<Link>> pairWait = Pair.of(waitingLink,
//								virtualNetwork.getVirtualNode(toNodeSecondIndex));
//
//						Triple<RoboTaxi, AVRequest, Pair<Link, VirtualNode<Link>>> pSOCommands = Triple
//								.of(closestRoboTaxi, avRequestSecond, pairWait);
//
//						pSOCommandsList.add(pSOCommands);
//
//						removePSOCommand(fromNode, toNodeFirst, ipso);
//
//					}
//				}
//			}
//		}
//
//		return pSOCommandsList;
//	}
//
//	List<List<double[]>> getControlLawPSO() {
//		return controlLaw;
//	}
//
//	HashMap<VirtualNode<Link>, List<Link>> getLinkMapPSO() {
//		return linkMap;
//	}
//
//	void removePSOCommand(VirtualNode<Link> fromNode, VirtualNode<Link> toNode, int toNodeSecond) {
//		controlLaw.get(fromNode.getIndex()).get(toNode.getIndex())[toNodeSecond] = 0;
//	}
//
//}
