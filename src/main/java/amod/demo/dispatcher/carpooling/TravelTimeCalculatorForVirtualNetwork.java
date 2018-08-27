package amod.demo.dispatcher.carpooling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.virtualnetwork.VirtualLink;

public enum TravelTimeCalculatorForVirtualNetwork {
    ;
    
static Map<VirtualLink<Link>,Double> computeTravelTimes(Collection<VirtualLink<Link>> vLinks){
        
        double speed = 12; //[m/s]
        Map<VirtualLink<Link>,Double> tTimes = new HashMap<>();
        
        for(VirtualLink<Link> vLink : vLinks){
            tTimes.put(vLink, vLink.getDistance()/speed);
        }
        
        return tTimes;
        
        
    }

}
