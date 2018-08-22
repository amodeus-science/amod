package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @SuppressWarnings("unchecked")
    List<Pair<RoboTaxi, Link>> getXDOCommands(VirtualNetwork<Link> virtualNetwork,
            Map<VirtualNode<Link>, List<RoboTaxi>> soRoboTaxi,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVFromRequests,
            Map<VirtualNode<Link>, List<AVRequest>> virtualNodeAVToRequests) throws Exception {

        List<Pair<RoboTaxi, Link>> xDOCommandsList = new ArrayList<>();

        for (VirtualNode<Link> destination : virtualNetwork.getVirtualNodes()) {
            List<double[]> controlLawDestination = controlLaw.get(destination.getIndex());
            if(controlLawDestination.isEmpty()) {
                continue;
            }
            for (int i = 0; i < controlLawDestination.size(); ++i) {
                double[] controlLawDestFrom = controlLawDestination.get(i);
                
                if(Arrays.stream(controlLawDestFrom).sum()==0) {
                    continue;
                }
                
                List<RoboTaxi> soTaxis = soRoboTaxi.get(virtualNetwork.getVirtualNode(i));
                List<RoboTaxi> availableCars = (List<RoboTaxi>) soTaxis.stream().filter(cars -> cars.getMenu().getCourses().size() == 1
                        && destination.getLinks().contains(cars.getCurrentDriveDestination())).collect(Collectors.toList());
                if(availableCars.isEmpty()) {
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

                    RoboTaxi nextRoboTaxi = availableCars.get(0);
                    availableCars.remove(nextRoboTaxi);
                    VirtualNode<Link> toNode = virtualNetwork.getVirtualNode((int) node);
                    Optional<Link> linkOption = toNode.getLinks().stream().findAny();

                    Pair<RoboTaxi, Link> xZOCommands = Pair.of(nextRoboTaxi, linkOption.get());
                    xDOCommandsList.add(xZOCommands);
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
        
        if(xDOCommandsList.isEmpty()) {
            return null;
        }

        return xDOCommandsList;
    }

    
}
