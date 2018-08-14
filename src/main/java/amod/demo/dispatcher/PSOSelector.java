package amod.demo.dispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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

            if (availableCars.isEmpty()) {
                System.out.println("No available cars for p_oz");
                continue;
            }

            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_oz");
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

                if (availableCars.size() >= controlLawSecondDestination.length) {
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
                        Pair<RoboTaxi, AVRequest> pSOCommands = Pair.of(RoboTaxi, fromToSecondRequests.get(0));
                        pSOCommandsList.add(pSOCommands);
                        car = car + 1;
                        ArrayUtils.add(removeElements, iteration);

                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);

                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                } else if (availableCars.size() < controlLawSecondDestination.length) {
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
                        Pair<RoboTaxi, AVRequest> pSOCommands = Pair.of(RoboTaxi, fromToSecondRequests.get(0));
                        pSOCommandsList.add(pSOCommands);
                        ArrayUtils.add(removeElements, iteration);
                        iteration = iteration + 1;
                    }
                    ArrayUtils.removeAll(controlLawSecondDestination, removeElements);
                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);

                }

            }
        }

        return pSOCommandsList;
    }

}
