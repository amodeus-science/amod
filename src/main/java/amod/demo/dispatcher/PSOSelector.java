package amod.demo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class PSOSelector {
    private List<List<double[]>> controlLaw;

    public PSOSelector(List<List<double[]>> controlLaw) {
        this.controlLaw = controlLaw;
    }

    @SuppressWarnings("unchecked")
    List<Pair<RoboTaxi, AVRequest>> getPSOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> SORoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) throws Exception {

        List<Pair<RoboTaxi, AVRequest>> pSOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> position : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> fromRequest = VirtualNodeAVFromRequests.get(position);
            List<double[]> controlLawFirstDestination = controlLaw.get(position.getIndex());
            List<RoboTaxi> availableCars = SORoboTaxi.get(position);

            List<List<AVRequest>> fromToRequestList = getFromToAVRequests(virtualNetwork, fromRequest,
                    VirtualNodeAVToRequests);

            if (availableCars.isEmpty()) {
                System.out.println("No available cars for p_so");
                continue;
            }

            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_so");
                continue;
            }

            for (int i = 0; i < controlLawFirstDestination.size(); ++i) {
                int ind = i;
                List<RoboTaxi> SOcars = (List<RoboTaxi>) availableCars.stream()
                        .filter(c -> virtualNetwork.getVirtualNode(c.getCurrentDriveDestination()).getIndex() == ind);
                if (SOcars.isEmpty()) {
                    continue;
                }

                double[] controlLawSecondDestination = controlLawFirstDestination.get(i);

                int iteration = 0;
                int[] removeElements = null;
                for (double node : controlLawSecondDestination) {
                    node = node - 1;
                    int indexNode = (int) node;

                    if (SOcars.isEmpty()) {
                        break;
                    }

                    List<AVRequest> fromToSecondRequests = fromToRequestList.get(indexNode);
                    if (fromToSecondRequests.isEmpty()) {
                        iteration = iteration + 1;
                        continue;
                    }

                    AVRequest avRequest = fromToSecondRequests.get(0);
                    fromToSecondRequests.remove(avRequest);
                    fromToRequestList.set(indexNode, fromToSecondRequests);

                    RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, SOcars);
                    SOcars.remove(closestRoboTaxi);
                    Pair<RoboTaxi, AVRequest> pSOCommands = Pair.of(closestRoboTaxi, fromToSecondRequests.get(0));
                    pSOCommandsList.add(pSOCommands);
                    ArrayUtils.add(removeElements, iteration);

                }
                ArrayUtils.removeAll(controlLawSecondDestination, removeElements);

                controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

            }
        }
        
        if(pSOCommandsList.isEmpty()) {
            return null;
        }

        return pSOCommandsList;
    }

    @SuppressWarnings("unchecked")
    private List<List<AVRequest>> getFromToAVRequests(VirtualNetwork<Link> virtualNetwork, List<AVRequest> fromRequest,
            Map<VirtualNode<Link>, List<AVRequest>> VirtualNodeAVToRequests) {
        List<List<AVRequest>> fromToAVRequests = new ArrayList<>(virtualNetwork.getvNodesCount());

        for (VirtualNode<Link> node : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> fromToRequests = new ArrayList<>();
            if(!fromRequest.isEmpty()) {
                fromToRequests = (List<AVRequest>) fromRequest.stream()
                        .filter(rt -> VirtualNodeAVToRequests.get(node).contains(rt));
            }
            if(!fromToRequests.isEmpty()) {
                fromToAVRequests.add(node.getIndex(), fromToRequests);
            }
               
        }

        return fromToAVRequests;

    }
}
