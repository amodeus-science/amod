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

public class XDOSelector {
    private List<List<double[]>> controlLaw;

    public XDOSelector(List<List<double[]>> controlLaw) {
        this.controlLaw = controlLaw;
    }

//    @SuppressWarnings("unchecked")
//    List<Triple<RoboTaxi, AVRequest, Link>> getXZOCommands(VirtualNetwork<Link> virtualNetwork,
//            Map<VirtualNode<Link>, List<RoboTaxi>> DORoboTaxi,
//            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests,
//            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests, List<Pair<Integer, Link>> listpair)
//            throws Exception {
//
//        List<Triple<RoboTaxi, AVRequest, Link>> xZOCommandsList = new ArrayList<>();
//
//        for (VirtualNode<Link> destination : virtualNetwork.getVirtualNodes()) {
//            List<AVRequest> requestDestination = VirtualNodeAVToRequests.get(destination);
//            List<double[]> controlLawDestination = controlLaw.get(destination.getIndex());
//            for (int i = 0; i < controlLawDestination.size(); ++i) {
//                double[] controlLawDestFrom = controlLawDestination.get(i);
//                List<RoboTaxi> availableCars = DORoboTaxi.get(virtualNetwork.getVirtualNode(i));
//                availableCars.stream().filter(c -> c.)
//                int fromNodeIndex = i;
//                List<AVRequest> fromToRequests = (List<AVRequest>) requestDestination.stream().filter(
//                        rt -> VirtualNodeAVFromRequests.get(virtualNetwork.getVirtualNode(fromNodeIndex)).contains(rt));
//                if (availableCars.isEmpty() == false && availableCars.size() >= controlLawDestFrom.length
//                        && availableCars.size() <= fromToRequests.size()) {
//                    int car = 0;
//                    for (double node : controlLawDestFrom) {
//                        node = node - 1;
//                        if (node < 0) {
//                            return null;
//                        }
//                        RoboTaxi RoboTaxi = availableCars.get(car);
//                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
//                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(RoboTaxi, fromToRequests.get(car),
//                                getLink(toNode.getIndex(), listpair));
//                        xZOCommandsList.add(xZOCommands);
//                        car = car + 1;
//                    }
//                    controlLawDestFrom = ArrayUtils.EMPTY_DOUBLE_ARRAY;
//
//                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);
//
//                } else if (availableCars.isEmpty() == false && availableCars.size() < controlLawDestFrom.length
//                        && availableCars.size() <= fromToRequests.size()) {
//                    double node;
//                    for (int icar = 0; icar < availableCars.size(); ++icar) {
//                        node = controlLawDestFrom[0] - 1;
//                        if (node < 0) {
//                            return null;
//                        }
//                        RoboTaxi RoboTaxi = availableCars.get(icar);
//                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
//                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(RoboTaxi, fromToRequests.get(icar),
//                                getLink(toNode.getIndex(), listpair));
//                        xZOCommandsList.add(xZOCommands);
//                        ArrayUtils.remove(controlLawDestFrom, 0);
//                    }
//                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);
//
//                }
//
//                else if (availableCars.isEmpty() == false && availableCars.size() >= controlLawDestFrom.length
//                        && availableCars.size() > fromToRequests.size()) {
//                    double node;
//                    for (int ireq = 0; ireq < fromToRequests.size(); ++ireq) {
//                        node = controlLawDestFrom[0] - 1;
//                        if (node < 0) {
//                            return null;
//                        }
//                        RoboTaxi RoboTaxi = availableCars.get(ireq);
//                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
//                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(RoboTaxi, fromToRequests.get(ireq),
//                                getLink(toNode.getIndex(), listpair));
//                        xZOCommandsList.add(xZOCommands);
//                        ArrayUtils.remove(controlLawDestFrom, 0);
//                    }
//                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);
//
//                }
//
//                else if (availableCars.isEmpty() == false && availableCars.size() < controlLawDestFrom.length
//                        && availableCars.size() > fromToRequests.size()) {
//                    double node;
//                    for (int ireq = 0; ireq < fromToRequests.size(); ++ireq) {
//                        node = controlLawDestFrom[0] - 1;
//                        if (node < 0) {
//                            return null;
//                        }
//                        RoboTaxi RoboTaxi = availableCars.get(ireq);
//                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
//                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(RoboTaxi, fromToRequests.get(ireq),
//                                getLink(toNode.getIndex(), listpair));
//                        xZOCommandsList.add(xZOCommands);
//                        ArrayUtils.remove(controlLawDestFrom, 0);
//                    }
//                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);
//
//                }
//            }
//        }
//
//        return xZOCommandsList;
//    }
//
//    private static Link getLink(int node, List<Pair<Integer, Link>> listpair) throws Exception {
//        for (Pair<Integer, Link> pair : listpair)
//            if (pair.getLeft() == node)
//                return pair.getRight();
//        throw new Exception("No equal node");
//    }
}
