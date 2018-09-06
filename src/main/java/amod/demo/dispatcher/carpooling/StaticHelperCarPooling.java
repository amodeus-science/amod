package amod.demo.dispatcher.carpooling;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.passenger.AVRequest;

enum StaticHelperCarPooling {
    ;

    static double distanceRobotaxiRequest(AVRequest avRequest, RoboTaxi roboTaxi) {
        return NetworkUtils.getEuclideanDistance( //
                avRequest.getFromLink().getCoord(), //
                roboTaxi.getDivertableLocation().getCoord());
    }
    
    static double distanceRequestRequest(AVRequest avRequest1, AVRequest request2) {
        return NetworkUtils.getEuclideanDistance( //
                avRequest1.getFromLink().getCoord(), //
                request2.getFromLink().getCoord());
    }

    static Set<Link> getCloseLinks(Coord coord, double distance, Network network) {
        Collection<Node> closeNodes = NetworkUtils.getNearestNodes(network, coord, distance);
        GlobalAssert.that(!closeNodes.isEmpty());
        Set<Link> closeLinks = network.getLinks().values().stream() //
                .filter(link -> closeNodes.contains(link.getFromNode())) //
                .filter(link -> closeNodes.contains(link.getToNode())) //
                .collect(Collectors.toSet());
        GlobalAssert.that(!closeLinks.isEmpty());
        return closeLinks;
    }

    static RoboTaxi findClostestVehicle(AVRequest avRequest, Collection<RoboTaxi> roboTaxis) {
        GlobalAssert.that(roboTaxis != null);
        RoboTaxi closestRoboTaxi = null;
        double min = Double.POSITIVE_INFINITY;
        for (RoboTaxi roboTaxi : roboTaxis) {
            double newDistance = distanceRobotaxiRequest(avRequest, roboTaxi);
            if (closestRoboTaxi == null || newDistance < min) {
                min = newDistance;
                closestRoboTaxi = roboTaxi;
            }
        }
        return closestRoboTaxi;
    }
    
    static RoboTaxi findClostestVehicleToLink(Link link, Collection<RoboTaxi> roboTaxis) {
        GlobalAssert.that(roboTaxis != null);
        RoboTaxi closestRoboTaxi = null;
        double min = Double.POSITIVE_INFINITY;
        for (RoboTaxi roboTaxi : roboTaxis) {
            double newDistance = NetworkUtils.getEuclideanDistance(link.getCoord(), roboTaxi.getDivertableLocation().getCoord());
            if (closestRoboTaxi == null || newDistance < min) {
                min = newDistance;
                closestRoboTaxi = roboTaxi;
            }
        }
        return closestRoboTaxi;
    }
    
    static AVRequest findClostestRequestfromVehcile(RoboTaxi roboTaxi, Collection<AVRequest> avRequest) {
        GlobalAssert.that(avRequest != null);
        AVRequest closestAVRequest = null;
        double min = Double.POSITIVE_INFINITY;
        for (AVRequest req : avRequest) {
            double newDistance = distanceRobotaxiRequest(req, roboTaxi);
            if (closestAVRequest == null || newDistance < min) {
                min = newDistance;
                closestAVRequest = req;
            }
        }
        return closestAVRequest;
    }
    
    static AVRequest findClostestRequestfromRequest(AVRequest avRequest1, Collection<AVRequest> avRequest2) {
        GlobalAssert.that(avRequest2 != null);
        AVRequest closestAVRequest = null;
        double min = Double.POSITIVE_INFINITY;
        for (AVRequest req : avRequest2) {
            double newDistance = distanceRequestRequest(avRequest1, req);
            if (closestAVRequest == null || newDistance < min) {
                min = newDistance;
                closestAVRequest = req;
            }
        }
        return closestAVRequest;
    }
    
    static Link findClostestWaitLink(AVRequest avRequest, Collection<Link> linkList) {
        GlobalAssert.that(linkList != null);
        Link closestLink = null;
        double min = Double.POSITIVE_INFINITY;
        for (Link link : linkList) {
            double newDistance = NetworkUtils.getEuclideanDistance( //
                    avRequest.getFromLink().getCoord(), //
                    link.getCoord());
            if (closestLink == null || newDistance < min) {
                min = newDistance;
                closestLink = link;
            }
        }
        return closestLink;
    }

    static Map<Link, Set<AVRequest>> getFromLinkMap(Collection<AVRequest> avRequests) {
        Map<Link, Set<AVRequest>> linkAVRequestMap = new HashMap<>();
        for (AVRequest avRequest : avRequests) {
            Link fromLink = avRequest.getFromLink();
            if (!linkAVRequestMap.containsKey(fromLink))
                linkAVRequestMap.put(fromLink, new HashSet<>());
            linkAVRequestMap.get(fromLink).add(avRequest);
        }
        return linkAVRequestMap;
    }

}

