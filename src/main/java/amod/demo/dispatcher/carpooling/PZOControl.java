package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class PZOControl {
    private List<List<double[]>> controlLaw;
    private HashMap<VirtualNode<Link>, List<Link>> linkMap;

    public PZOControl(List<List<double[]>> controlLaw, HashMap<VirtualNode<Link>, List<Link>> linkMap) {
        this.controlLaw = controlLaw;
        this.linkMap = linkMap;
    }

    List<Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>>> getPZOCommands(
            VirtualNetwork<Link> virtualNetwork, Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests) throws Exception {

        List<Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>>> pZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> fromNode : virtualNetwork.getVirtualNodes()) {
            List<RoboTaxi> availableCars = stayRoboTaxi.get(fromNode);
            if (availableCars.isEmpty()) {
                continue;
            }
            List<AVRequest> fromRequests = virtualNodeAVFromRequests.get(fromNode);
            if (fromRequests.isEmpty()) {
                continue;
            }
            for (VirtualNode<Link> toNodeFirst : virtualNetwork.getVirtualNodes()) {
                
                if(availableCars.isEmpty()) {
                    break;
                }
                
                double[] controlPzo = controlLaw.get(fromNode.getIndex()).get(toNodeFirst.getIndex());

                if (Arrays.stream(controlPzo).sum() == 0) {
                    continue;
                }

                List<AVRequest> toRequestFistUnfilterd = virtualNodeAVToRequests.get(toNodeFirst);
                List<AVRequest> toRequestFirst = fromRequests.stream()
                        .filter(req -> toRequestFistUnfilterd.contains(req)).collect(Collectors.toList());

                for (int ipzo = 0; ipzo < controlPzo.length; ipzo++) {
                    int toNodeSecondIndex = (int) controlPzo[ipzo] - 1;
                    if (toNodeSecondIndex < 0) {
                        continue;
                    }
                    
                    if(availableCars.isEmpty()) {
                        break;
                    }

                    List<AVRequest> toRequestSecondUnfilterd = virtualNodeAVToRequests
                            .get(virtualNetwork.getVirtualNode(toNodeSecondIndex));
                    List<AVRequest> toRequestSecond = fromRequests.stream()
                            .filter(req -> toRequestSecondUnfilterd.contains(req)).collect(Collectors.toList());
                    if (!toRequestFirst.isEmpty() && !toRequestSecond.isEmpty()
                            && toNodeSecondIndex != toNodeFirst.getIndex()) {
                        AVRequest avRequestFirst = toRequestFirst.get(0);
                        toRequestFirst.remove(avRequestFirst);
                        virtualNodeAVFromRequests.get(fromNode).remove(avRequestFirst);
                        virtualNodeAVToRequests.get(toNodeFirst).remove(avRequestFirst);

                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestFirst,
                                availableCars);
                        availableCars.remove(closestRoboTaxi);
                        stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);

                        AVRequest avRequestSecond = StaticHelperCarPooling
                                .findClostestRequestfromRequest(avRequestFirst, toRequestSecond);
                        toRequestSecond.remove(avRequestSecond);
                        toRequestSecondUnfilterd.remove(avRequestSecond);
                        virtualNodeAVFromRequests.get(fromNode).remove(avRequestSecond);
                        virtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(toNodeSecondIndex))
                                .remove(avRequestSecond);

                        Pair<AVRequest, AVRequest> pairRequests = Pair.of(avRequestFirst, avRequestSecond);
                        Pair<Link, VirtualNode<Link>> pairWait = Pair.of(null, null);

                        Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> pZOCommands = Triple
                                .of(closestRoboTaxi, pairRequests, pairWait);

                        pZOCommandsList.add(pZOCommands);

                        removePZOCommand(fromNode, toNodeFirst, ipzo);

                    }
                    else if (!toRequestFirst.isEmpty() && toRequestSecond.isEmpty()) {
                        AVRequest avRequestFirst = toRequestFirst.get(0);
                        toRequestFirst.remove(avRequestFirst);
                        virtualNodeAVFromRequests.get(fromNode).remove(avRequestFirst);
                        virtualNodeAVToRequests.get(toNodeFirst).remove(avRequestFirst);

                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestFirst,
                                availableCars);
                        availableCars.remove(closestRoboTaxi);
                        stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);

                        List<Link> linkSet = linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex));
                        List<Link> fromNodeLinkList = linkSet.stream()
                                .filter(link -> fromNode.getLinks().contains(link)).collect(Collectors.toList());
                        
                        Link waitingLink = null;
                        if(!fromNodeLinkList.isEmpty()) {
                            waitingLink = StaticHelperCarPooling.findClostestWaitLink(avRequestFirst,
                                    fromNodeLinkList);
                            linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex)).remove(waitingLink);
                        } else {
                            waitingLink = avRequestFirst.getFromLink();
                        }
                        
                        Pair<Link, VirtualNode<Link>> pairWait = Pair.of(waitingLink,
                                virtualNetwork.getVirtualNode(toNodeSecondIndex));

                        Pair<AVRequest, AVRequest> pairRequests = Pair.of(avRequestFirst, null);
                        Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> pZOCommands = Triple
                                .of(closestRoboTaxi, pairRequests, pairWait);

                        pZOCommandsList.add(pZOCommands);

                        removePZOCommand(fromNode, toNodeFirst, ipzo);

                    }
                    else if (toRequestFirst.isEmpty() && !toRequestSecond.isEmpty()) {
                        AVRequest avRequestFirst = null;

                        AVRequest avRequestSecond = toRequestSecond.get(0);
                        toRequestSecond.remove(avRequestSecond);
                        toRequestSecondUnfilterd.remove(avRequestSecond);
                        virtualNodeAVFromRequests.get(fromNode).remove(avRequestSecond);
                        virtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(toNodeSecondIndex))
                                .remove(avRequestSecond);

                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestSecond,
                                availableCars);
                        availableCars.remove(closestRoboTaxi);
                        stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);

                        List<Link> linkSet = linkMap.get(toNodeFirst);
                        List<Link> fromNodeLinkList = linkSet.stream()
                                .filter(link -> fromNode.getLinks().contains(link)).collect(Collectors.toList());

                        Link waitingLink = null;
                        if(!fromNodeLinkList.isEmpty()) {
                            waitingLink = StaticHelperCarPooling.findClostestWaitLink(avRequestSecond,
                                    fromNodeLinkList);
                            linkMap.get(toNodeFirst).remove(waitingLink);
                        } else {
                            waitingLink = avRequestSecond.getFromLink();
                        }

                        Pair<Link, VirtualNode<Link>> pairWait = Pair.of(waitingLink, toNodeFirst);

                        Pair<AVRequest, AVRequest> pairRequests = Pair.of(avRequestFirst, avRequestSecond);
                        Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> pZOCommands = Triple
                                .of(closestRoboTaxi, pairRequests, pairWait);

                        pZOCommandsList.add(pZOCommands);

                        removePZOCommand(fromNode, toNodeFirst, ipzo);
                    }
                    else if (!toRequestFirst.isEmpty() && !toRequestSecond.isEmpty()
                            && toNodeSecondIndex == toNodeFirst.getIndex()) {

                        if (toRequestFirst.size() == 1) {
                            AVRequest avRequestFirst = toRequestFirst.get(0);
                            toRequestFirst.remove(avRequestFirst);
                            virtualNodeAVFromRequests.get(fromNode).remove(avRequestFirst);
                            virtualNodeAVToRequests.get(toNodeFirst).remove(avRequestFirst);

                            RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestFirst,
                                    availableCars);
                            availableCars.remove(closestRoboTaxi);
                            stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);

                            List<Link> linkSet = linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex));
                            List<Link> fromNodeLinkList = linkSet.stream()
                                    .filter(link -> fromNode.getLinks().contains(link)).collect(Collectors.toList());

                            Link waitingLink = StaticHelperCarPooling.findClostestWaitLink(avRequestFirst,
                                    fromNodeLinkList);
                            linkMap.get(virtualNetwork.getVirtualNode(toNodeSecondIndex)).remove(waitingLink);

                            Pair<Link, VirtualNode<Link>> pairWait = Pair.of(waitingLink,
                                    virtualNetwork.getVirtualNode(toNodeSecondIndex));

                            Pair<AVRequest, AVRequest> pairRequests = Pair.of(avRequestFirst, null);
                            Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> pZOCommands = Triple
                                    .of(closestRoboTaxi, pairRequests, pairWait);

                            pZOCommandsList.add(pZOCommands);

                            removePZOCommand(fromNode, toNodeFirst, ipzo);
                        }
                        else {
                            AVRequest avRequestFirst = toRequestFirst.get(0);
                            toRequestFirst.remove(avRequestFirst);
                            virtualNodeAVFromRequests.get(fromNode).remove(avRequestFirst);
                            virtualNodeAVToRequests.get(toNodeFirst).remove(avRequestFirst);
                            toRequestSecond.remove(avRequestFirst);
                            toRequestSecondUnfilterd.remove(avRequestFirst);

                            RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestFirst,
                                    availableCars);
                            availableCars.remove(closestRoboTaxi);
                            stayRoboTaxi.get(fromNode).remove(closestRoboTaxi);

                            AVRequest avRequestSecond = StaticHelperCarPooling
                                    .findClostestRequestfromRequest(avRequestFirst, toRequestSecond);
                            toRequestSecond.remove(avRequestSecond);
                            toRequestSecondUnfilterd.remove(avRequestSecond);
                            virtualNodeAVFromRequests.get(fromNode).remove(avRequestSecond);
                            virtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(toNodeSecondIndex))
                                    .remove(avRequestSecond);
                            toRequestFirst.remove(avRequestSecond);

                            Pair<AVRequest, AVRequest> pairRequests = Pair.of(avRequestFirst, avRequestSecond);
                            Pair<Link, VirtualNode<Link>> pairWait = Pair.of(null, null);

                            Triple<RoboTaxi, Pair<AVRequest, AVRequest>, Pair<Link, VirtualNode<Link>>> pZOCommands = Triple
                                    .of(closestRoboTaxi, pairRequests, pairWait);

                            pZOCommandsList.add(pZOCommands);

                            removePZOCommand(fromNode, toNodeFirst, ipzo);
                        }
                        
                    }

                }
            }
        }

        return pZOCommandsList;
    }

    List<List<double[]>> getControlLawPZO() {
        return controlLaw;
    }

    HashMap<VirtualNode<Link>, List<Link>> getLinkMapPZO() {
        return linkMap;
    }

    void removePZOCommand(VirtualNode<Link> fromNode, VirtualNode<Link> toNode, int toNodeSecond) {
        controlLaw.get(fromNode.getIndex()).get(toNode.getIndex())[toNodeSecond] = 0;
    }

}
