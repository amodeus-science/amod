package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests) throws Exception {

        List<Triple<RoboTaxi, AVRequest, AVRequest>> pZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> position : virtualNetwork.getVirtualNodes()) {

            List<AVRequest> fromRequest = virtualNodeAVFromRequests.get(position);
            List<RoboTaxi> availableCars = stayRoboTaxi.get(position);

            if (availableCars.isEmpty()) {
                System.out.println("No available cars for p_zo");
                continue;
            }

            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_zo");
                continue;
            }

            List<List<AVRequest>> fromToRequestList = CarPooling2DispatcherUtils.getFromToAVRequests(virtualNetwork, fromRequest,
                    virtualNodeAVToRequests);
            List<double[]> controlLawFirstDestination = controlLaw.get(position.getIndex());
            
            if(controlLawFirstDestination.isEmpty()) {
                continue;
            }

            for (int i = 0; i < controlLawFirstDestination.size(); ++i) {
                double[] controlLawSecondDestination = controlLawFirstDestination.get(i);
                
                if(controlLawSecondDestination.equals(ArrayUtils.EMPTY_DOUBLE_ARRAY)) {
                    continue;
                }


                List<AVRequest> fromToFirstRequests = fromToRequestList.get(i);
                if (fromToFirstRequests.isEmpty()) {
                    continue;
                }

                int iteration = 0;
                List<Integer> removeElements = new ArrayList<Integer>();
                for (double node : controlLawSecondDestination) {
                    node = node - 1;
                    int indexNode = (int) node;
                    
                    if(node<1) {
                        iteration = iteration + 1;
                        continue;
                    }

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
                    removeElements.add(iteration);
                    iteration = iteration + 1;

                }
                if(!removeElements.isEmpty()) {
                    for(int removeArray: removeElements) {
                        controlLawSecondDestination[removeArray] = 0;
                    }

                    controlLaw.get(position.getIndex()).set(i, controlLawSecondDestination);
                }
                

            }
        }

        if(pZOCommandsList.isEmpty()) {
            return null;
        }
        
        return pZOCommandsList;
    }
  
}
