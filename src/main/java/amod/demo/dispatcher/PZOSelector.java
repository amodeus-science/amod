package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
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

    List<Triple<RoboTaxi, AVRequest, AVRequest>> getPZOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> StayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) throws Exception {

        List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> position : virtualNetwork.getVirtualNodes()) {

            List<AVRequest> fromRequest = VirtualNodeAVFromRequests.get(position);
            List<RoboTaxi> availableCars = StayRoboTaxi.get(position);

            if (availableCars.isEmpty()) {
                System.out.println("No available cars for p_oz");
                continue;
            }

            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_oz");
                continue;
            }

            List<List<AVRequest>> fromToRequestList = getFromToAVRequests(virtualNetwork, fromRequest,
                    VirtualNodeAVToRequests);
            List<double[]> controlLawFirstDestination = controlLaw.get(position.getIndex());

            for (int i = 0; i < controlLawFirstDestination.size(); ++i) {
                double[] controlLawSecondDestination = controlLawFirstDestination.get(i);

                List<AVRequest> fromToFirstRequests = fromToRequestList.get(i);
                if (fromToFirstRequests.isEmpty()) {
                    continue;
                }

                int iteration = 0;
                int[] removeElements = null;
                for (double node : controlLawSecondDestination) {
                    node = node - 1;
                    int indexNode = (int) node;

                    if (availableCars.isEmpty()) {
                        break;
                    }

                    if (fromToFirstRequests.isEmpty()) {
                        break;
                    }
                    
                    if(i==indexNode && fromToRequestList.get(indexNode).size() < 2) {
                        iteration = iteration + 1;
                        continue;
                    }
                    
                    if(i!=indexNode && fromToRequestList.get(indexNode).isEmpty()) {
                        iteration = iteration + 1;
                        continue;
                    }

                    AVRequest avRequestFirst = fromToFirstRequests.get(0);
                    fromToFirstRequests.remove(avRequestFirst);
                    fromToRequestList.set(i, fromToFirstRequests);

                    RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequestFirst,
                            availableCars);
                    availableCars.remove(closestRoboTaxi);

                    List<AVRequest> fromToSecondRequests = fromToRequestList.get(indexNode);

                    AVRequest avRequestSecond = fromToSecondRequests.get(0);
                    fromToSecondRequests.remove(avRequestSecond);
                    fromToRequestList.set(indexNode, fromToSecondRequests);

                    Triple<RoboTaxi, AVRequest, AVRequest> pZOCommands = Triple.of(closestRoboTaxi, avRequestFirst,
                            avRequestSecond);

                    pZOCommandsList.add(pZOCommands);
                    ArrayUtils.add(removeElements, iteration);

                }
                ArrayUtils.removeAll(controlLawSecondDestination, removeElements);

                controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

            }
        }

        return pZOCommandsList;
    }

    private List<List<AVRequest>> getFromToAVRequests(VirtualNetwork<Link> virtualNetwork, List<AVRequest> fromRequest,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) {
        List<List<AVRequest>> fromToAVRequests = new ArrayList<>();

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            @SuppressWarnings("unchecked")
            List<AVRequest> fromToRequests = (List<AVRequest>) fromRequest.stream()
                    .filter(rt -> VirtualNodeAVToRequests.get(node).contains(rt));
            fromToAVRequests.add(node.getIndex(), fromToRequests);
        }

        return fromToAVRequests;

    }
}
