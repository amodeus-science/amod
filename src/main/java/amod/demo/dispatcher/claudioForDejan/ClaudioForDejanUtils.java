package amod.demo.dispatcher.claudioForDejan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import amod.demo.dispatcher.SMPCDispatcher;
import amod.demo.ext.UserReferenceFrames;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiPlanEntry;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;

public enum ClaudioForDejanUtils {
;
    
    public static Container getContainerInit(){
        
        return new Container("InputSMPC");
        

    }
    
    public static boolean testtest(double[] testnode, double[] testdist, int index, double dist) {
        if(testnode.length != testdist.length) {
            System.out.println("not same length");
            return false;
        }
        
        int numb = testnode.length;
        for(int i=0; i<numb; ++i) {
            if(testnode[i]==index && testdist[i]==dist) {
                return true;
            }
        }
        
        return false;
    }
    
    public static List<Id<Node>> getNetworkNodeList(Network network){
        List<Id<Node>> listNodes = new ArrayList<Id<Node>>();
        int indexNode = 0;
        for(Id<Node> node: network.getNodes().keySet()) {           
            listNodes.add(indexNode, node);
            indexNode += 1;
        }
        
        return listNodes;
    }
    
    public static List<Id<Node>> getNetworkReducedNodeList(Network network){
        List<Id<Node>> listNodes = new ArrayList<Id<Node>>();
        int indexNode = 0;
        for(Node node: network.getNodes().values()) {
            if(node.getInLinks().size()>=2 || node.getOutLinks().size()>=2) {
                listNodes.add(indexNode, node.getId());
                indexNode += 1;
            }
        }
        
        return listNodes;
    }
    
    public static List<double[]> getNetworkForMatlab(Network network, List<Id<Node>> listNodes){
               
        List<double[]> listNodesAdjacancy = new ArrayList<double[]>();
        
        for(Node node: network.getNodes().values()) {
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesAdjacancy.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    Id<Node> toNode = linkOut.getToNode().getId();
                    adjacancy[i] = listNodes.indexOf(toNode);
                    i += 1;
                }
                listNodesAdjacancy.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
        
        return listNodesAdjacancy;      

    }
    
    public static List<double[]> getReducedNetwork(Network network, List<Id<Node>> listNodes){
        
        List<double[]> listNodesInter = new ArrayList<double[]>();
 
        for(Id<Node> nodeID: listNodes) {
            Node node = network.getNodes().get(nodeID);
            int size = node.getOutLinks().values().size();
            int i = 0;
            double[] adjacancy = new double[size];
            Node nodeIt = null;
            for(Link linkOut: node.getOutLinks().values()) {
                Node nodeTo = linkOut.getToNode();
                nodeIt = nodeTo;
                while(nodeIt.getOutLinks().size()<2 && nodeIt.getInLinks().size()<2 && !nodeIt.getOutLinks().values().isEmpty()) {
                    
                    nodeIt = nodeIt.getOutLinks().values().iterator().next().getToNode();
                    
                }
                
                Id<Node> toNode = nodeIt.getId();
                adjacancy[i] = listNodes.indexOf(toNode);
                i += 1;
            }
            listNodesInter.add(listNodes.indexOf(node.getId()),adjacancy);
        }
        
//        for(Node node: network.getNodes().values()) {
//            if(node.getInLinks().size()>=2 || node.getOutLinks().size()>+2) {
//                Coord coordNode = node.getCoord();
//                Coord coordNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordNode);
//                double[] location = new double[2];
//                location[0] = coordNodeWGS84.getY();
//                location[1] = coordNodeWGS84.getX();
//                listNodesInter.add(location);
//            }
//            
//        }
        
        return listNodesInter;      

    }
    
public static List<double[]> getReducedNetworkDistance(Network network, List<Id<Node>> listNodes){
        
        List<double[]> listNodesInter = new ArrayList<double[]>();
 
        for(Id<Node> nodeID: listNodes) {
            Node node = network.getNodes().get(nodeID);
            int size = node.getOutLinks().values().size();
            int i = 0;
            double[] adjacancy = new double[size];
            Node nodeIt = null;
            double distance = 0;
            for(Link linkOut: node.getOutLinks().values()) {
                Node nodeTo = linkOut.getToNode();
                distance = linkOut.getLength()/3.28084;
                nodeIt = nodeTo;
                while(nodeIt.getOutLinks().size()<2 && nodeIt.getInLinks().size()<2 && !nodeIt.getOutLinks().values().isEmpty()) { 
                    Link link = nodeIt.getOutLinks().values().iterator().next();
                    distance = distance + link.getLength()/3.28084;
                    nodeIt = link.getToNode();
                    
                }
                
                adjacancy[i] = distance;
                i += 1;
            }
            listNodesInter.add(listNodes.indexOf(node.getId()),adjacancy);
        }
        
//        for(Node node: network.getNodes().values()) {
//            if(node.getInLinks().size()>=2 || node.getOutLinks().size()>+2) {
//                Coord coordNode = node.getCoord();
//                Coord coordNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordNode);
//                double[] location = new double[2];
//                location[0] = coordNodeWGS84.getY();
//                location[1] = coordNodeWGS84.getX();
//                listNodesInter.add(location);
//            }
//            
//        }
        
        return listNodesInter;      

    }
    
    public static List<double[]> getNetworkDistanceForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesDistance = new ArrayList<double[]>();
        
        for(Node node: network.getNodes().values()) {
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesDistance.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    adjacancy[i] = linkOut.getLength()/3.28084;
                    i += 1;
                }
                listNodesDistance.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
 
        return listNodesDistance;

    }
    
    public static List<double[]> getNetworkVelocityForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesVelocity = new ArrayList<double[]>();
        
        for(Node node: network.getNodes().values()) {
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesVelocity.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    adjacancy[i] = linkOut.getFreespeed();
                    i += 1;
                }
                listNodesVelocity.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
 
        return listNodesVelocity;

    }
    
    public static List<double[]> getReducedNetworkVelocityForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesVelocity = new ArrayList<double[]>();
        
        for(Id<Node> nodeID: listNodes) {
            Node node = network.getNodes().get(nodeID);
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesVelocity.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    adjacancy[i] = linkOut.getFreespeed();
                    i += 1;
                }
                listNodesVelocity.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
 
        return listNodesVelocity;

    }
    
    public static List<double[]> getNetworkCapacityForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesCapacity = new ArrayList<double[]>();
        
        for(Node node: network.getNodes().values()) {
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesCapacity.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    adjacancy[i] = linkOut.getCapacity()/(60*60);
                    i += 1;
                }
                listNodesCapacity.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
 
        return listNodesCapacity;

    }
    
    public static List<double[]> getReducedNetworkCapacityForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesCapacity = new ArrayList<double[]>();
        
        for(Id<Node> nodeID: listNodes) {
            Node node = network.getNodes().get(nodeID);
            if(node.getOutLinks().isEmpty() == true) {
                double[] adjacancy = {};
                listNodesCapacity.add(adjacancy);
            }
            else {
                int size = node.getOutLinks().values().size();
                double[] adjacancy = new double[size];
                int i = 0;
                for(Link linkOut: node.getOutLinks().values()) {
                    adjacancy[i] = linkOut.getCapacity()/(60*60);
                    i += 1;
                }
                listNodesCapacity.add(listNodes.indexOf(node.getId()),adjacancy);
            } 
            
        }
 
        return listNodesCapacity;

    }
    
    public static List<double[]> getNetworkLocationForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesLocation = new ArrayList<double[]>();
        
        for(Node node: network.getNodes().values()) {
            Coord coordNode = node.getCoord();
            Coord coordNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordNode);
            double[] location = new double[2];
            location[0] = coordNodeWGS84.getY();
            location[1] = coordNodeWGS84.getX();
            listNodesLocation.add(listNodes.indexOf(node.getId()), location);
        }
 
        return listNodesLocation;

    }
    
    public static List<double[]> getReducedNetworkLocationForMatlab(Network network, List<Id<Node>> listNodes){
        List<double[]> listNodesLocation = new ArrayList<double[]>();
        
        for(Id<Node> nodeID: listNodes) {
            Node node = network.getNodes().get(nodeID);
            Coord coordNode = node.getCoord();
            Coord coordNodeWGS84 = UserReferenceFrames.SANFRANCISCO.coords_toWGS84().transform(coordNode);
            double[] location = new double[2];
            location[0] = coordNodeWGS84.getY();
            location[1] = coordNodeWGS84.getX();
            listNodesLocation.add(listNodes.indexOf(node.getId()), location);
        }
 
        return listNodesLocation;

    }
    
    public static List<double[]> getRequestMatlab(Network network, List<Id<Node>> listNodes, Config config, double round_now){
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
        List<double[]> dataList = new ArrayList<>();
                                
        int helper = 0;
        int FromnodeIndex = 0;
        int TonodeIndex = 0;
        
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if(activity.getEndTime() >= round_now && activity.getEndTime()<= (round_now+2*60*60) ) {                                           
                            if (!stageActivityTypes.isStageActivity(activity.getType())) {
                                Link link = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                if (link != null) {
                                    Id<Node> linkID = link.getFromNode().getId();
                                    FromnodeIndex = listNodes.indexOf(linkID);
                                    helper = 1;
                                } 
                            }
                        }
                        if(activity.getStartTime() != Double.NEGATIVE_INFINITY && helper == 1) {
                            if(!stageActivityTypes.isStageActivity(activity.getType())) {
                                Link link = network.getLinks().getOrDefault(activity.getLinkId(), null);
                                if(link != null) {
                                    Id<Node> linkID = link.getToNode().getId();
                                    TonodeIndex = listNodes.indexOf(linkID);
                                    dataList.add(new double[] {FromnodeIndex, TonodeIndex});
                                    helper = 0;
                                } 
                            }
                        }
                    }
                    
                }
            }
        }
 
        return dataList;

    }
    
    public static double[][] getVirtualNetworkForMatlab(VirtualNetwork<Link> virtualNetwork){
        int NodeNumber = virtualNetwork.getVirtualNodes().size();
        double[][] VirtualNetworkMatrix = new double[NodeNumber][NodeNumber];
        
        for(VirtualLink<Link> link: virtualNetwork.getVirtualLinks()) {
            int FromNode = link.getFrom().getIndex();
            int ToNode = link.getTo().getIndex();
            VirtualNetworkMatrix[FromNode][ToNode] = ToNode + 1;
        }
        
        for(int i = 0; i<VirtualNetworkMatrix.length; ++i) {
            VirtualNetworkMatrix[i][i] = i+1;
        }
        
        
        return VirtualNetworkMatrix;      

    }
    
    public static double[][] getTravelTimesForMatlab(VirtualNetwork<Link> virtualNetwork, Map<VirtualLink<Link>, Double> TravelTimes){
        
        int NodeNumber = virtualNetwork.getVirtualNodes().size();
        double[][] travelTimesMat = new double[NodeNumber][NodeNumber];
        
        for(VirtualLink<Link> link: TravelTimes.keySet()) {
            int FromNode = link.getFrom().getIndex();
            int ToNode = link.getTo().getIndex();
            travelTimesMat[FromNode][ToNode] = Math.round(TravelTimes.get(link)/(5*60));
            
        }
        
        for(int i = 0; i<travelTimesMat.length; ++i) {
            travelTimesMat[i][i] = 1;
        }
        
        return travelTimesMat;      

    }
    
    public static double[][] getTotalAvailableCarsForMatlab(double Time, int PlanningHorizon, Map<VirtualNode<Link>,List<RoboTaxi>> IdleCars,
        Map<VirtualNode<Link>,List<RoboTaxi>> RebalanceCars, Map<VirtualNode<Link>,List<RoboTaxi>> CustomerCars){
        
        int NumberNodes = IdleCars.keySet().size();
        
        if(RebalanceCars.keySet().size() != NumberNodes || CustomerCars.keySet().size() != NumberNodes) {
            throw new RuntimeException();
        }
        
        double[][] TotalAvailableCars = new double[PlanningHorizon][NumberNodes];
        int numberIdle;
        int numberReb;
        int numberCust;
        
        for(VirtualNode<Link> node: IdleCars.keySet()) {
            List<RoboTaxi> IdleCarsAtNode = IdleCars.get(node);
            List<RoboTaxi> RebCarsAtNode = RebalanceCars.get(node);
            List<RoboTaxi> CustCarsAtNode = CustomerCars.get(node);
            
            if(IdleCarsAtNode.isEmpty() == true) {
                numberIdle = 0;
            }
            else {
                numberIdle = IdleCarsAtNode.size();
            }
            
            for(int t=0; t<PlanningHorizon; t++) {
                if(RebCarsAtNode.isEmpty() == true) {
                    numberReb = 0;
                }
                else {
                    numberReb = getNumberCarsAbailableAtTime(Time, t, RebCarsAtNode);
                }
                
                if(CustCarsAtNode.isEmpty() == true) {
                    numberCust = 0;
                }
                else {
                    numberCust = getNumberCarsAbailableAtTime(Time, t, CustCarsAtNode); 
                }
                
                if(t==0) {
                    TotalAvailableCars[t][node.getIndex()] = numberIdle + numberReb + numberCust;
                }
                else {
                    TotalAvailableCars[t][node.getIndex()] = numberReb + numberCust;
                }
                             
            }
            
             
        }
        
        return TotalAvailableCars;      

    }

    private static int getNumberCarsAbailableAtTime(double Time, int t, List<RoboTaxi> carsAtNode) {
        int numberCars = 0;
        for(RoboTaxi car: carsAtNode) {
            Collection<RoboTaxiPlanEntry> plansCollection = car.getCurrentPlans(Time).getPlans().values();
            for(RoboTaxiPlanEntry planEntry: plansCollection) {
                if(planEntry.endTime > Time + t*5*60 && planEntry.endTime <= Time+(t+1)*5*60 && car.getStatus() == planEntry.status) {
                    numberCars = numberCars + 1;
                }
            }
        }
        return numberCars;
    }
    
    public static void printArray(Container container, String field) {
        if (container.contains(field)) {
          DoubleArray doubleArray = container.get(field);
          System.out.println(doubleArray);
          for (int index = 0; index < doubleArray.value.length; ++index)
            System.out.print("[" + index + "]=" + doubleArray.value[index] + ", ");
          System.out.println();
        } else {
          System.out.println(" !!! field '" + field + "' not present !!! ");
        }
    }
    
    public static double[] getArray(Container container, String field) {
        if (container.contains(field)) {
          DoubleArray doubleArray = container.get(field);
          double[] array = doubleArray.value;
          return array;
        } else {
          throw new EmptyStackException();
        }

    }
}
