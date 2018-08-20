package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class XZOSelector {
    private List<List<double[]>> controlLaw;

    public XZOSelector(List<List<double[]>> controlLaw) {
        this.controlLaw = controlLaw;
    }

    @SuppressWarnings("unchecked")
    List<Triple<RoboTaxi, AVRequest, Link>> getXZOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests)
            throws Exception {

        List<Triple<RoboTaxi, AVRequest, Link>> xZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> destination : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> requestDestination = VirtualNodeAVToRequests.get(destination);
            List<double[]> controlLawDestination = controlLaw.get(destination.getIndex());
            for (int i = 0; i < controlLawDestination.size(); ++i) {
                double[] controlLawDestFrom = controlLawDestination.get(i);
                List<RoboTaxi> availableCars = StayRoboTaxi.get(virtualNetwork.getVirtualNode(i));
                if (availableCars.isEmpty()) {
                    System.out.println("No available cars for x_zo");
                    continue;
                }
                
                int fromNodeIndex = i;
                List<AVRequest> fromToRequests = (List<AVRequest>) requestDestination.stream().filter(
                        rt -> VirtualNodeAVFromRequests.get(virtualNetwork.getVirtualNode(fromNodeIndex)).contains(rt));
                
                if (fromToRequests.isEmpty()) {
                    System.out.println("No available requests for x_zo");
                    continue;
                }
                if (availableCars.size() >= controlLawDestFrom.length
                        && availableCars.size() <= fromToRequests.size()) {
                    int car = 0;
                    for (double node : controlLawDestFrom) {
                        node = node - 1;
                        if (node < 0) {
                            return null;
                        }
                        AVRequest avRequest = fromToRequests.get(car);
                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
                        availableCars.remove(closestRoboTaxi);
                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                        Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest,
                                linkOption.get());
                        xZOCommandsList.add(xZOCommands);
                        car = car + 1;
                    }
                    controlLawDestFrom = ArrayUtils.EMPTY_DOUBLE_ARRAY;

                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);

                } else if (availableCars.size() < controlLawDestFrom.length
                        && availableCars.size() <= fromToRequests.size()) {
                    double node;
                    for (int icar = 0; icar < availableCars.size(); ++icar) {
                        node = controlLawDestFrom[0] - 1;
                        if (node < 0) {
                            return null;
                        }
                        AVRequest avRequest = fromToRequests.get(icar);
                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
                        availableCars.remove(closestRoboTaxi);
                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                        Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest,
                                linkOption.get());
                        xZOCommandsList.add(xZOCommands);
                        ArrayUtils.remove(controlLawDestFrom, 0);
                    }
                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);

                }

                else if (availableCars.size() >= controlLawDestFrom.length
                        && availableCars.size() > fromToRequests.size()) {
                    double node;
                    for (int ireq = 0; ireq < fromToRequests.size(); ++ireq) {
                        node = controlLawDestFrom[0] - 1;
                        if (node < 0) {
                            return null;
                        }
                        AVRequest avRequest = fromToRequests.get(ireq);
                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
                        availableCars.remove(closestRoboTaxi);
                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                        Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest,
                                linkOption.get());
                        xZOCommandsList.add(xZOCommands);
                        ArrayUtils.remove(controlLawDestFrom, 0);
                    }
                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);

                }

                else if (availableCars.size() < controlLawDestFrom.length
                        && availableCars.size() > fromToRequests.size()) {
                    double node;
                    for (int ireq = 0; ireq < fromToRequests.size(); ++ireq) {
                        node = controlLawDestFrom[0] - 1;
                        if (node < 0) {
                            return null;
                        }
                        AVRequest avRequest = fromToRequests.get(ireq);
                        RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
                        availableCars.remove(closestRoboTaxi);
                        VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                        Optional<Link> linkOption = toNode.getLinks().stream().findAny();
                        Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest,
                                linkOption.get());
                        xZOCommandsList.add(xZOCommands);
                        ArrayUtils.remove(controlLawDestFrom, 0);
                    }
                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);

                }
            }
        }

        if(xZOCommandsList.isEmpty()) {
            return null;
        }
        System.out.println(xZOCommandsList.get(0).getRight());
        return xZOCommandsList;
    }

}
