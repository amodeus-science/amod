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
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ class FindCongestionIterative {

    private final TripComparisonMaintainer tripMaintainer;
    private final ApplyScaling applyScaling = new ApplyScaling();

    /** These diff values should converge to 1 */
    public Scalar costMid = RealScalar.of(100); // any high enough number ok as initialization

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
        Collections.shuffle(allTrips, random); // <-- necessary? TODO
        this.tripMaintainer = new TripComparisonMaintainer(allTrips, network, db);

        /** export initial distribution */
        StaticHelper.exportRatioMap(tripMaintainer.getLookupMap(), "Initial");

        /** show initial score */
        System.out.println("cost initial: " + costFunction.apply(tripMaintainer.getLookupMap()));

        while (iterations < maxIter && Scalars.lessEquals(tolerance, costMid)) {
            ++iterations;
            tripIteration(allTrips);
            /** intermediate export */
            StaticHelper.export(processingDir, lsData, "_" + Integer.toString(iterations));
        }

        applyScaling.printIncreaseMap();
        System.out.println("cost End: " + costFunction.apply(tripMaintainer.getLookupMap()));

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
            if (Scalars.lessEquals(randomS, epsilon2)) { // take next random trip
                trip = allTrips.get(tripCount);
                ++tripCount;
            } else { // take worst of existing trips
                trip = tripMaintainer.getWorst();
                System.out.println("reselecting " + trip.localId);
            }

            /** create the shortest duration calculator using the linkSpeed data,
             * must be done again to take into account newest updates */
            DurationCompare compare = getPathDurationRatio(trip);
            Scalar pathDurationratio = compare.nwPathDurationRatio;

            Scalar ratioBefore = pathDurationratio;

            /** rescale factor such that epsilon in [0,1] maps to [f,1] */
            Scalar rescaleFactor = RealScalar.ONE.subtract(//
                    (RealScalar.ONE.subtract(pathDurationratio)).multiply(epsilon1));

            /** rescale all links */
            applyScaling.to(lsData, trip, compare.path, rescaleFactor, dt);

            compare = getPathDurationRatio(trip);
            pathDurationratio = compare.nwPathDurationRatio;

            if (!ratioDidImprove(ratioBefore, pathDurationratio)) {
                // if(true){
                System.err.println("trip:        " + trip.localId);
                System.err.println("ratioBefore: " + ratioBefore);
                System.err.println("ratioAfter:  " + pathDurationratio);
                System.err.println("Now at trip " + trip.localId);
                System.err.println("Now at trip " + trip.pickupLoc);
                System.err.println("Now at trip " + trip.dropoffLoc);
                System.err.println("compare.path");
                compare.path.links.forEach(l -> System.err.println(l.getId().toString()));
                System.err.println("compare.pathDist " + compare.pathDist);
                System.err.println("compare.duration " + compare.duration);
                System.err.println("compare.pathTime " + compare.pathTime);
            }

            tripMaintainer.update(trip, pathDurationratio);

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
            if (tripCount % 10 == 0) {
                costMid = costFunction.apply(tripMaintainer.getLookupMap());
                System.out.println("trip:       " + tripCount + " / " + allTrips.size());
                System.out.println("cost:       " + costMid);
                System.out.println("worst cost: " + tripMaintainer.getWorstCost());
                System.out.println("worst trip: " + tripMaintainer.getWorst().localId);
                if (Scalars.lessEquals(costMid, tolerance))
                    break;
            }

            // DEBUGGING
            /** DEBUGGING every 1000 trips, export cost map */
            if (tripCount % 500 == 0) {
                StaticHelper.exportRatioMap(tripMaintainer.getLookupMap(), Integer.toString(tripCount));
            }
            // DEBUGGING END

            System.out.println("----");

        }

    }

    private DurationCompare getPathDurationRatio(TaxiTrip trip) {
        /** create the shortest duration calculator using the linkSpeed data,
         * must be done again to take into account newest updates */
        LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
        ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, network, db);
        /** comupte ratio of network path and trip duration f */
        DurationCompare comp = new DurationCompare(trip, calc);
        return comp;
    }

    private boolean ratioDidImprove(Scalar ratioBefore, Scalar ratioAfter) {
        Scalar s1 = ratioBefore.subtract(RealScalar.ONE).abs();
        Scalar s2 = ratioAfter.subtract(RealScalar.ONE).abs();
        return Scalars.lessThan(s2, s1);
    }

}
