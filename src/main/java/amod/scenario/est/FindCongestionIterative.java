/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;

import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ class FindCongestionIterative {

    // --
    private final Map<TaxiTrip, Scalar> ratioLookupMap = new HashMap<>();
    private final NavigableMap<Scalar, TaxiTrip> ratioSortedMap = new TreeMap<>();
    /** These diff values should converge to 1 */
    public Scalar costMid = RealScalar.of(100); // any high enough number ok as initialization
    // public final Scalar costEnd;
    /** settings and data */
    private final Scalar tolerance;
    private final Network network;
    private final LinkSpeedDataContainer lsData;
    private final MatsimAmodeusDatabase db;
    private final Function<Map<TaxiTrip, Scalar>, Scalar> costFunction;
    /** this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is adviced o chose slow. No changes
     * are applied for epsilon == 0. */
    private final Scalar epsilon1;
    private final Scalar epsilon2;
    private final Random random;
    private final int dt;

    public FindCongestionIterative(Network network, MatsimAmodeusDatabase db, File processingDir, //
            LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
            int maxIter, Scalar tol, Scalar epsilon1, Scalar epsilon2, Random random, int dt, //
            Function<Map<TaxiTrip, Scalar>, Scalar> costFunction) {
        this.network = network;
        this.db = db;
        this.tolerance = Objects.requireNonNull(tol);
        this.lsData = lsData;
        this.epsilon1 = epsilon1;
        this.epsilon2 = epsilon2;
        this.random = random;
        this.dt = dt;
        this.costFunction = costFunction;
        int iterations = 0;

        /** export the initial distribution of ratios */
        Collections.shuffle(allTrips, random);
        ShortestDurationCalculator calc = new ShortestDurationCalculator(network, db);
        // LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
        // ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, network, db);
        int count = 0;
        for (TaxiTrip trip : allTrips) {
            ++count;
            if (count % 100 == 0)
                System.out.println("Freespeed length calculation: " + count);
            DurationCompare compare = new DurationCompare(trip, calc);
            Scalar pathDurationratio = compare.nwPathDurationRatio;
            ratioLookupMap.put(trip, pathDurationratio);
            ratioSortedMap.put(pathDurationratio.subtract(RealScalar.ONE).abs(), trip);

            //
            // if(Scalars.lessEquals(compare.pathTime, Quantity.of(10, "s"))){
            // System.err.println("Now at trip " + trip.localId);
            // System.err.println("Now at trip " + trip.pickupLoc);
            // System.err.println("Now at trip " + trip.dropoffLoc);
            // System.err.println("compare.path");
            // compare.path.links.forEach(l->System.err.println(l.getId().toString()));
            // System.err.println("compare.pathDist " + compare.pathDist);
            // System.err.println("compare.nwPathDurationRatio " + compare.nwPathDurationRatio);
            // System.err.println("compare.nwPathDurationRatio " + compare.nwPathDurationRatio);
            // }

        }

        /** export initial distribution */
        StaticHelper.exportRatioMap(ratioLookupMap, "Initial");

        /** show initial score */
        System.out.println("cost initial: " + costFunction.apply(ratioLookupMap));

        while (iterations < maxIter && Scalars.lessEquals(tolerance, costMid)) {
            ++iterations;
            tripIteration(allTrips);
            /** intermediate export */
            StaticHelper.export(processingDir, lsData, "_" + Integer.toString(iterations));
        }

        System.out.println("cost End: " + costFunction.apply(ratioLookupMap));

    }

    /** All available {@link TaxiTrip}s in @param allTrips
     * are used to estimate link speeds. */
    private void tripIteration(List<TaxiTrip> allTrips) {
        /** shuffle trips to have random order */
        Collections.shuffle(allTrips, random);
        int tripCount = 0;
        int totalTrips = allTrips.size();
        while (tripCount < totalTrips - 1) {
            /** decide if new random trip or worst of existing trips */
            Scalar randomS = RealScalar.of(random.nextDouble());
            TaxiTrip trip = null;
            if (Scalars.lessEquals(randomS, epsilon2) || ratioSortedMap.size() == 0) { // take next random trip
                trip = allTrips.get(tripCount);
                ++tripCount;
            } else { // take worst of existing trips
                trip = ratioSortedMap.lastEntry().getValue();
            }

            /** create the shortest duration calculator using the linkSpeed data,
             * must be done again to take into account newest updates */
            LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
            ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, network, db);

            /** comupte ratio of network path and trip duration f */
            DurationCompare comp = new DurationCompare(trip, calc);
            Scalar pathDurationratio = comp.nwPathDurationRatio;
            ratioLookupMap.put(trip, pathDurationratio);
            ratioSortedMap.put(pathDurationratio.subtract(RealScalar.ONE).abs(), trip);
            /** rescale factor such that epsilon in [0,1] maps to [f,1] */
            Scalar rescaleFactor = RealScalar.ONE.subtract(//
                    (RealScalar.ONE.subtract(pathDurationratio)).multiply(epsilon1));

            /** rescale all links */
            ApplyScaling.to(lsData, trip, comp.path, rescaleFactor, dt);

            // // DEBUGGING
            // {
            // DurationCompare compNew = new DurationCompare(trip, calc);
            // Scalar pathDurationratioNew = comp.nwPathDurationRatio;
            // if (Scalars.lessEquals(RealScalar.ONE, pathDurationratio)) {
            // System.err.println("rescaleFactor: " + rescaleFactor);
            // System.err.println("new ratio: " + pathDurationratioNew);
            // }
            // // GlobalAssert.that(Scalars.lessEquals(pathDurationratio, RealScalar.ONE));
            // ratioMap.put(trip, pathDurationratioNew);
            // }
            // // DEUBBING END

            /** assess every 20 trips if ok */
            if (tripCount % 50 == 0) {
                costMid = costFunction.apply(ratioLookupMap);
                System.out.println("trip: " + tripCount + " / " + allTrips.size());
                System.out.println("cost: " + costMid);
                if (Scalars.lessEquals(costMid, tolerance))
                    break;
            }

            // DEBUGGING
            /** DEBUGGING every 1000 trips, export cost map */
            if (tripCount % 100 == 0) {
                StaticHelper.exportRatioMap(ratioLookupMap, Integer.toString(tripCount));
            }
            // DEBUGGING END
        }
    }

}
