package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests) throws Exception {

        List<Pair<RoboTaxi, AVRequest>> pSOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> position : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> fromRequest = virtualNodeAVFromRequests.get(position);
            
            if (fromRequest.isEmpty()) {
                System.out.println("No available requests for p_so");
                continue;
            }
            
            List<double[]> controlLawFirstDestination = controlLaw.get(position.getIndex());
            
            if(controlLawFirstDestination.isEmpty()) {
                continue;
            }
            
            List<RoboTaxi> soTaxis = soRoboTaxi.get(position);
            
            if (soTaxis.isEmpty()) {
                System.out.println("No available cars for p_so");
                continue;
            }
            
            List<List<AVRequest>> fromToRequestList = CarPooling2DispatcherUtils.getFromToAVRequests(virtualNetwork, fromRequest,
                    virtualNodeAVToRequests);


            for (int i = 0; i < controlLawFirstDestination.size(); ++i) {
                int ind = i;
                
                List<RoboTaxi> availableCars = soTaxis.stream()
                        .filter(c -> virtualNetwork.getVirtualNode(c.getCurrentDriveDestination()).getIndex() == ind && c.getMenu().getCourses().size() == 1).collect(Collectors.toList());
               
                if (availableCars.isEmpty()) {
                    continue;
                }

                double[] controlLawSecondDestination = controlLawFirstDestination.get(i);
                
                if(controlLawSecondDestination.equals(ArrayUtils.EMPTY_DOUBLE_ARRAY)) {
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

                    List<AVRequest> fromToSecondRequests = fromToRequestList.get(indexNode);
                    if (fromToSecondRequests.isEmpty()) {
                        iteration = iteration + 1;
                        continue;
                    }

                    AVRequest avRequest = fromToSecondRequests.get(0);
                    fromToSecondRequests.remove(avRequest);
                    fromToRequestList.set(indexNode, fromToSecondRequests);

                    RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest, availableCars);
                    availableCars.remove(closestRoboTaxi);
                    Pair<RoboTaxi, AVRequest> pSOCommands = Pair.of(closestRoboTaxi, avRequest);
                    pSOCommandsList.add(pSOCommands);
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
        
        if(pSOCommandsList.isEmpty()) {
            return null;
        }

        return pSOCommandsList;
    }

}
