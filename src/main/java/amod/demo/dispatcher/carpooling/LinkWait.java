package amod.demo.dispatcher.carpooling;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;

public class LinkWait {
    private HashMap<VirtualNode<Link>, List<Link>> linkMap;
    
    public LinkWait(HashMap<VirtualNode<Link>, List<Link>> linkMap) {
        this.linkMap = linkMap;
    }
    
    public HashMap<VirtualNode<Link>, List<Link>> getLinkWait(){
        return linkMap;
    }
    
    public void setLinkWait(HashMap<VirtualNode<Link>, List<Link>> linkMap) {
        this.linkMap = linkMap;
    }
    
    public void addLinkWaitElement(VirtualNode<Link> toNode, Link link) {
        linkMap.get(toNode).add(link);
    }

}
