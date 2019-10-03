package amod.scenario.population;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.geo.ClosestLinkSelect;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ class TaxiDistanceCalculator {

    private final Map<String, NavigableSet<TaxiTrip>> tripMap = new HashMap<>();
    private final LeastCostPathCalculator lcpc;
    private final ClosestLinkSelect linkSelect;
    private final File exportFolder;

    public TaxiDistanceCalculator(File exportFolder, Network network, //
            ClosestLinkSelect linkSelect) {
        this.exportFolder = exportFolder;
        this.lcpc = new FastAStarLandmarksFactory()//
                .createPathCalculator(network, new DistanceAsTravelDisutility(), //
                        new FreeSpeedTravelTime());
        this.linkSelect = linkSelect;
    }

    public void addTrip(TaxiTrip taxiTrip) {
        if (!tripMap.containsKey(taxiTrip.taxiId)) {
            // taxi trips are sorted according to pickup date
            tripMap.put(taxiTrip.taxiId, new TreeSet<>());
        }
        tripMap.get(taxiTrip.taxiId).add(taxiTrip);
    }

    public void exportTotalDistance() throws IOException {
        Map<String, Tensor> totalDistances = new HashMap<>();
        for (String taxiId : tripMap.keySet()) {
            // initialize
            Scalar taxiTotDist = Quantity.of(0, "km");
            Scalar taxiEmptyDistance = Quantity.of(0, "km");

            // calculate minimum trip and intra trip distances
            TaxiTrip tripBefore = null;
            for (TaxiTrip trip : tripMap.get(taxiId)) {
                // distance of current trip
                Link tStart = linkSelect.linkFromWGS84(trip.pickupLoc);
                Link tDesti = linkSelect.linkFromWGS84(trip.dropoffLoc);
                Scalar tripDist = shortPathDistance(tStart, tDesti);
                taxiTotDist = taxiTotDist.add(tripDist);
                if (Objects.nonNull(tripBefore)) {
                    // intra trip distance
                    Link tDestidBefore = linkSelect.linkFromWGS84(tripBefore.dropoffLoc);
                    Scalar intraTripDist = shortPathDistance(tDestidBefore, tStart);
                    taxiTotDist = taxiTotDist.add(intraTripDist);
                    taxiEmptyDistance = taxiEmptyDistance.add(intraTripDist);
                }
                tripBefore = trip;
            }
            totalDistances.put(taxiId, Tensors.of(taxiTotDist, taxiEmptyDistance));
        }
        // export
        Tensor export = Tensors.empty();
        Scalar fleetDistance = Quantity.of(0, "km");
        Scalar fleetDistanceEmpty = Quantity.of(0, "km");
        for (String id : totalDistances.keySet()) {
            export.append(Tensors.of(Scalars.fromString(id), totalDistances.get(id)));
            fleetDistance = fleetDistance.add(totalDistances.get(id).Get(0));
            fleetDistanceEmpty = fleetDistanceEmpty.add(totalDistances.get(id).Get(1));
        }
        export.append(Tensors.of(Scalars.fromString("{fleet}"), Tensors.of(fleetDistance, fleetDistanceEmpty)));
        Export.of(new File(exportFolder, "minFleetDistance.csv"), export);
        int totalTrips = tripMap.values().stream().mapToInt(s -> s.size()).sum();
        System.out.println("Total trips: " + totalTrips);
    }

    private Scalar shortPathDistance(Link linkStart, Link to) {
        Path shortest = lcpc.calcLeastCostPath(linkStart.getFromNode(), to.getToNode(), 1, null, null);
        double distance = 0.0;
        for (Link link : shortest.links)
            distance += link.getLength();
        return Quantity.of(distance / 1000.0, "km");

    }
}
