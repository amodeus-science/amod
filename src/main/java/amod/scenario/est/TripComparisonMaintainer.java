package amod.scenario.est;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;

public class TripComparisonMaintainer {

    // TODO can maintain same size more pretty? 
    private final Map<TaxiTrip, Scalar> ratioLookupMap = new HashMap<>();
    private NavigableMap<Scalar, TaxiTrip> ratioSortedMap = new TreeMap<>();

    public TripComparisonMaintainer(List<TaxiTrip> allTrips, Network network, MatsimAmodeusDatabase db) {
        // initial fill
        ShortestDurationCalculator calc = new ShortestDurationCalculator(network, db);
        int count = 0;
        for (TaxiTrip trip : allTrips) {
            ++count;
            if (count % 100 == 0)
                System.out.println("Freespeed length calculation: " + count);
            DurationCompare compare = new DurationCompare(trip, calc);
            Scalar pathDurationratio = compare.nwPathDurationRatio;
            ratioLookupMap.put(trip, pathDurationratio);
            Scalar cost = pathDurationratio.subtract(RealScalar.ONE).abs();
            ratioSortedMap.put(cost, trip);
        }
        GlobalAssert.that(ratioSortedMap.size() <= ratioLookupMap.size());
    }

    public void update(TaxiTrip trip, Scalar pathDurationratio) {
        // remove using old value
        Scalar ratioBefore = ratioLookupMap.get(trip);
        Scalar costBefore = ratioBefore.subtract(RealScalar.ONE).abs();
        // update worst trip
        if(!ratioSortedMap.remove(costBefore, trip)){
            System.out.println("Cleansing sorted map...");
            // if not removed successfully, remove all values associated to this trip.
            NavigableMap<Scalar, TaxiTrip> ratioSortedMapOld = ratioSortedMap;
            ratioSortedMap = new TreeMap<>();
            ratioSortedMapOld.entrySet().forEach(e->{
                if(!e.getValue().equals(trip)){
                    ratioSortedMap.put(e.getKey(), e.getValue());
                }
            });
        }
        ratioSortedMap.put(pathDurationratio.subtract(RealScalar.ONE).abs(), trip);
        // update lookupMap
        ratioLookupMap.put(trip, pathDurationratio);
        GlobalAssert.that(ratioSortedMap.size() <= ratioLookupMap.size());
    }

    public TaxiTrip getWorst() {
        return ratioSortedMap.lastEntry().getValue();
    }

    public Scalar getWorstCost() {
        return ratioSortedMap.lastEntry().getKey();
    }

    public Map<TaxiTrip, Scalar> getLookupMap() {
        return Collections.unmodifiableMap(ratioLookupMap);
    }

}
