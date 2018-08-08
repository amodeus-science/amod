package amod.demo.router;

import java.net.Socket;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import amod.demo.dispatcher.claudioForDejan.ClaudioForDejanUtils;
import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.jmex.java.JavaContainerSocket;
import ch.ethz.idsc.jmex.matlab.MfileContainerServer;

public class FlowDependentTravelDisutility implements TravelDisutility {

    private static final Logger log = Logger.getLogger(FlowDependentTravelDisutility.class);
    
    protected final Network network;

    public FlowDependentTravelDisutility(final Network network) {
        this.network = network;
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) { 
        try {
            // initialize server
            JavaContainerSocket javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

            { // add inputs to server
            Container container = new Container("Network");
            double[] initToll = new double[]{0};
            container.add((new DoubleArray("Disutility", new int[] { 1 }, initToll )));
            
            Id<Node> fromNode = link.getFromNode().getId();
            Id<Node> toNode = link.getToNode().getId();
            List<Id<Node>> listNodes = ClaudioForDejanUtils.getNetworkNodeList(this.network);
            double[] fromNodeID = new double[] {listNodes.indexOf(fromNode)};
            double[] toNodeID = new double[] {listNodes.indexOf(toNode)};
            container.add((new DoubleArray("FromNode", new int[] { 1 }, fromNodeID )));
            container.add((new DoubleArray("ToNode", new int[] { 1 }, toNodeID )));
            
            
            
            System.out.println("Sending to server");
            javaContainerSocket.writeContainer(container);
            
            }
            
            { // get outputs from server
            System.out.println("Waiting for server");
            Container container = javaContainerSocket.blocking_getContainer();
            System.out.println("received: " + container);
                                   
            double[] toll = ClaudioForDejanUtils.getArray(container, "solution");
            javaContainerSocket.close();
            return toll[0];
            }
                   
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(); // dispatcher will not work if
                                          // constructor has issues
        }        
        
    }

    @Override
    public double getLinkMinimumTravelDisutility(final Link link) {
        try {
            // initialize server
            JavaContainerSocket javaContainerSocket = new JavaContainerSocket(new Socket("localhost", MfileContainerServer.DEFAULT_PORT));

            { // add inputs to server
            Container container = new Container("Network");
            double[] initToll = new double[]{0};
            container.add((new DoubleArray("Disutility", new int[] { 1 }, initToll )));
            
            Id<Node> fromNode = link.getFromNode().getId();
            Id<Node> toNode = link.getToNode().getId();
            List<Id<Node>> listNodes = ClaudioForDejanUtils.getNetworkNodeList(this.network);
            double[] fromNodeID = new double[] {listNodes.indexOf(fromNode)};
            double[] toNodeID = new double[] {listNodes.indexOf(toNode)};
            container.add((new DoubleArray("FromNode", new int[] { 1 }, fromNodeID )));
            container.add((new DoubleArray("ToNode", new int[] { 1 }, toNodeID )));
            
            
            
            System.out.println("Sending to server");
            javaContainerSocket.writeContainer(container);
            
            }
            
            { // get outputs from server
            System.out.println("Waiting for server");
            Container container = javaContainerSocket.blocking_getContainer();
            System.out.println("received: " + container);
                                   
            double[] toll = ClaudioForDejanUtils.getArray(container, "solution");
            javaContainerSocket.close();
            return toll[0];
            }
            
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(); // dispatcher will not work if
                                          // constructor has issues
        }        
    }
    
}