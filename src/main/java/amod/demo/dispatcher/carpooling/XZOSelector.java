package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.matsim.av.passenger.AVRequest;

public class XZOSelector {
    private List<List<double[]>> controlLaw;
    private List<RoboTaxi> availableCars;
    private List<AVRequest> fromToRequests;

    public XZOSelector(List<List<double[]>> controlLaw) {
        this.controlLaw = controlLaw;
    }

    List<Triple<RoboTaxi, AVRequest, Link>> getXZOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> stayRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests)
            throws Exception {

        List<Triple<RoboTaxi, AVRequest, Link>> xZOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> destination : virtualNetwork.getVirtualNodes()) {
            List<AVRequest> requestDestination = virtualNodeAVToRequests.get(destination);
            
            if(requestDestination.isEmpty()) {
                continue;
            }
            
            List<double[]> controlLawDestination = controlLaw.get(destination.getIndex());
            
            if(controlLawDestination.isEmpty()) {
                continue;
            }
            
            for (int i = 0; i < controlLawDestination.size(); ++i) {
                double[] controlLawDestFrom = controlLawDestination.get(i);
                
                if(Arrays.stream(controlLawDestFrom).sum()==0) {
                    continue;
                }
                
                availableCars = stayRoboTaxi.get(virtualNetwork.getVirtualNode(i));
                if (availableCars.isEmpty()) {
                    continue;
                }
                
                int fromNodeIndex = i;
                
                fromToRequests = requestDestination.stream().filter(
                        rt -> virtualNodeAVFromRequests.get(virtualNetwork.getVirtualNode(fromNodeIndex)).contains(rt)).collect(Collectors.toList());
                     
                
                if (fromToRequests.isEmpty()) {
                    continue;
                }
                
 
                int iteration = 0;
                List<Integer> removeElements = new ArrayList<Integer>();
                for (double node : controlLawDestFrom) {
                    node = node - 1;
                    int indexNode = (int) node;
                    
                    if(indexNode<0) {
                        iteration = iteration + 1;
                        continue;
                    }

                    if (availableCars.isEmpty()) {
                        break;
                    }

                    if (fromToRequests.isEmpty()) {
                        break;
                    }
                      

                    AVRequest avRequest = fromToRequests.get(0);
                    fromToRequests.remove(avRequest);
                    virtualNodeAVToRequests.get(destination).remove(avRequest);

                    RoboTaxi closestRoboTaxi = StaticHelperCarPooling.findClostestVehicle(avRequest,
                            availableCars);
                    availableCars.remove(closestRoboTaxi);
                    stayRoboTaxi.get(virtualNetwork.getVirtualNode(i)).remove(closestRoboTaxi);
                    
                    VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                    Set<Link> linkSet = toNode.getLinks();
                    List<Link> linkList = new ArrayList<Link>(linkSet);
                    List<Link> linkListFiltered = linkList.stream().filter(link -> link!=null).collect(Collectors.toList());
                    Link redirectLink = linkListFiltered.get(new Random().nextInt(linkListFiltered.size()-1));

                    Triple<RoboTaxi, AVRequest, Link> xZOCommands = Triple.of(closestRoboTaxi, avRequest,
                            redirectLink);
                    xZOCommandsList.add(xZOCommands);
                    removeElements.add(iteration);
                    iteration = iteration + 1;

                }
                
                if(!removeElements.isEmpty()) {
                    for(int removeArray: removeElements) {
                        controlLawDestFrom[removeArray] = 0;
                    }

                    controlLaw.get(destination.getIndex()).set(i, controlLawDestFrom);
                }
                
            }
        }

        if(xZOCommandsList.isEmpty()) {
            return null;
        }
        
        
        return xZOCommandsList;
    }

}
