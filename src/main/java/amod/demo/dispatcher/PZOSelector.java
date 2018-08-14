package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class PZOSelector {
    private List<List<double[]>> controlLaw;

    public PZOSelector(List<List<double[]>> controlLaw) {
        this.controlLaw = controlLaw;
    }

    @SuppressWarnings("unchecked")
    List<Triple<RoboTaxi, AVRequest, AVRequest>> getPZOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests, List<Pair<Integer, Link>> listpair)
            throws Exception {

        List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> position : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> fromRequest = VirtualNodeAVFromRequests.get(position);
            List<double[]> controlLawFirstDestination = controlLaw.get(position.getIndex());
            List<RoboTaxi> availableCars = StayRoboTaxi.get(position);

            if (availableCars.isEmpty()) {
                System.out.println("No available cars for p_oz");
                continue;
            }

            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_oz");
                continue;
            }

            for (int i = 0; i < controlLawFirstDestination.size(); ++i) {
                double[] controlLawSecondDestination = controlLawFirstDestination.get(i);

                int toFirstNodeIndex = i;
                List<AVRequest> fromToFirstRequests = (List<AVRequest>) fromRequest.stream()
                        .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(toFirstNodeIndex))
                                .contains(rt));
                if (fromToFirstRequests.isEmpty()) {
                    continue;
                }
                if (availableCars.size() >= controlLawSecondDestination.length
                        && fromToFirstRequests.size() >= controlLawSecondDestination.length) {
                    int car = 0;
                    int iteration = 0;
                    int[] removeElements = null;
                    for (double node : controlLawSecondDestination) {
                        node = node - 1;
                        if (node < 0) {
                            return null;
                        }
                        int indexNode = (int) node;
                        List<AVRequest> fromToSecondRequests = (List<AVRequest>) fromRequest.stream()
                                .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(indexNode))
                                        .contains(rt));
                        if (fromToSecondRequests.isEmpty()) {
                            iteration = iteration + 1;
                            continue;
                        }
                        RoboTaxi RoboTaxi = availableCars.get(car);
                        Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(RoboTaxi,
                                fromToFirstRequests.get(car), fromToSecondRequests.get(0));
                        pZOCommandsList.add(pZOCommands);
                        car = car + 1;
                        ArrayUtils.add(removeElements, iteration);
                        
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);

                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                } else if (availableCars.size() < controlLawSecondDestination.length
                        && fromToFirstRequests.size() >= controlLawSecondDestination.length) {
                    double node;
                    int iteration = 0;
                    int[] removeElements = null;
                    for (int icar = 0; icar < availableCars.size(); ++icar) {
                        node = controlLawSecondDestination[icar] - 1;
                        if (node < 0) {
                            return null;
                        }
                        int indexNode = (int) node;
                        List<AVRequest> fromToSecondRequests = (List<AVRequest>) fromRequest.stream()
                                .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(indexNode))
                                        .contains(rt));
                        
                        if (fromToSecondRequests.isEmpty()) {
                            iteration = iteration + 1;
                            continue;
                        }
                        RoboTaxi RoboTaxi = availableCars.get(icar);
                        Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(RoboTaxi,
                                fromToFirstRequests.get(icar), fromToSecondRequests.get(0));
                        pZOCommandsList.add(pZOCommands);
                        ArrayUtils.add(removeElements, iteration);
                        iteration = iteration + 1;
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);
                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                }

                else if (availableCars.size() >= controlLawSecondDestination.length
                        && fromToFirstRequests.size() < controlLawSecondDestination.length) {
                    double node;
                    int iteration = 0;
                    int[] removeElements = null;
                    for (int ireq = 0; ireq < fromToFirstRequests.size(); ++ireq) {
                        node = controlLawSecondDestination[ireq] - 1;
                        if (node < 0) {
                            return null;
                        }
                        int indexNode = (int) node;
                        List<AVRequest> fromToSecondRequests = (List<AVRequest>) fromRequest.stream()
                                .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(indexNode))
                                        .contains(rt));
                        if (fromToSecondRequests.isEmpty()) {
                            iteration = iteration + 1;
                            continue;
                        }
                        RoboTaxi RoboTaxi = availableCars.get(ireq);
                        Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(RoboTaxi,
                                fromToFirstRequests.get(ireq), fromToSecondRequests.get(ireq));
                        pZOCommandsList.add(pZOCommands);
                        ArrayUtils.add(removeElements, iteration);
                        iteration = iteration + 1;
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);
                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                }

                else if (availableCars.size() < controlLawSecondDestination.length
                        && fromToFirstRequests.size() < controlLawSecondDestination.length && availableCars.size() >= fromToFirstRequests.size()) {
                    double node;
                    int iteration = 0;
                    int[] removeElements = null;
                    for (int ireq = 0; ireq < fromToFirstRequests.size(); ++ireq) {
                        node = controlLawSecondDestination[ireq] - 1;
                        if (node < 0) {
                            return null;
                        }
                        int indexNode = (int) node;
                        List<AVRequest> fromToSecondRequests = (List<AVRequest>) fromRequest.stream()
                                .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(indexNode))
                                        .contains(rt));
                        if (fromToSecondRequests.isEmpty()) {
                            iteration = iteration + 1;
                            continue;
                        }
                        RoboTaxi RoboTaxi = availableCars.get(ireq);
                        Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(RoboTaxi,
                                fromToFirstRequests.get(ireq), fromToSecondRequests.get(ireq));
                        pZOCommandsList.add(pZOCommands);
                        ArrayUtils.add(removeElements, iteration);
                        iteration = iteration + 1;
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);
                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                }
                
                else if (availableCars.size() < controlLawSecondDestination.length
                        && fromToFirstRequests.size() < controlLawSecondDestination.length && availableCars.size() <= fromToFirstRequests.size()) {
                    double node;
                    int iteration = 0;
                    int[] removeElements = null;
                    for (int icar = 0; icar < availableCars.size(); ++icar) {
                        node = controlLawSecondDestination[icar] - 1;
                        if (node < 0) {
                            return null;
                        }
                        int indexNode = (int) node;
                        List<AVRequest> fromToSecondRequests = (List<AVRequest>) fromRequest.stream()
                                .filter(rt -> VirtualNodeAVToRequests.get(virtualNetwork.getVirtualNode(indexNode))
                                        .contains(rt));
                        if (fromToSecondRequests.isEmpty()) {
                            iteration = iteration + 1;
                            continue;
                        }
                        RoboTaxi RoboTaxi = availableCars.get(icar);
                        Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(RoboTaxi,
                                fromToFirstRequests.get(icar), fromToSecondRequests.get(icar));
                        pZOCommandsList.add(pZOCommands);
                        ArrayUtils.add(removeElements, iteration);
                        iteration = iteration + 1;
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);
                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                }
            }
        }

        return pZOCommandsList;
    }

    private static Link getLink(int node, List<Pair<Integer, Link>> listpair) throws Exception {
        for (Pair<Integer, Link> pair : listpair)
            if (pair.getLeft() == node)
                return pair.getRight();
        throw new Exception("No equal node");
    }
}
