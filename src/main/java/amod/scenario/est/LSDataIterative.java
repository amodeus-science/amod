/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;

import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.io.SaveFormats;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ class LSDataIterative {

    // --
    private final Map<TaxiTrip, Scalar> ratioMap = new HashMap<>();
    /** These diff values should converge to 1 */
    public Scalar costMid = RealScalar.of(100); // any high enough number ok as initialization
    public final Scalar costEnd;
    /** settings and data */
    private final Scalar tolerance;
    private final Network network;
    private final LinkSpeedDataContainer lsData;
    private final MatsimAmodeusDatabase db;
    private final Function<Map<TaxiTrip, Scalar>, Scalar> cost;
    /** this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is adviced o chose slow. No changes
     * are applied for epsilon == 0. */
    private final Scalar epsilon;
    private final Random random;
    private final int dt;

    public LSDataIterative(Network network, MatsimAmodeusDatabase db, File processingDir, //
            LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
            int maxIter, Scalar tol, Scalar epsilon, Random random, int dt, //
            Function<Map<TaxiTrip, Scalar>, Scalar> cost) {
        this.network = network;
        this.db = db;
        this.tolerance = tol;
        this.lsData = lsData;
        this.epsilon = epsilon;
        this.random = random;
        this.dt = dt;
        this.cost = cost;

        int iterations = 0;

        while (iterations < maxIter && !isBelowTolerance(costMid, tolerance)) {
            ++iterations;
            tripIteration(allTrips);
            /** intermediate export */
            StaticHelper.export(processingDir, lsData, "_" + Integer.toString(iterations));
        }

        costEnd = cost.apply(ratioMap);
        System.out.println("diffEnd: " + costEnd);

    }

    /** All available {@link TaxiTrip}s in @param allTrips
     * are used to estimate link speeds. */
    private void tripIteration(List<TaxiTrip> allTrips) {
        /** shuffle trips to have random order */
        Collections.shuffle(allTrips, random);

        int tripCount = 0;
        for (TaxiTrip trip : allTrips) {
            ++tripCount;

            /** create the shortest duration calculator using the linkSpeed data,
             * must be done again to take into account newest updates */
            LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
            ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, network, db);

            /** comupte ratio of network path and trip duration f */
            DurationCompare comp = new DurationCompare(trip, calc);
            Scalar pathDurationratio = comp.nwPathDurationRatio;
            ratioMap.put(trip, pathDurationratio);
            /** rescale factor such that epsilon in [0,1] maps to [f,1] */
            Scalar rescaleFactor = RealScalar.ONE.subtract(//
                    (RealScalar.ONE.subtract(pathDurationratio)).multiply(epsilon));

            /** rescale all links */
            ApplyScaling.to(lsData, trip, comp.path, rescaleFactor, dt);

            /** assess every 20 trips if ok */
            if (tripCount % 50 == 0) {
                costMid = cost.apply(ratioMap);
                System.out.println("trip: " + tripCount + " / " + allTrips.size());
                System.out.println("costMid: " + costMid);
                if (isBelowTolerance(costMid, tolerance))
                    break;
            }

            /** DEBUGGING every 100 trips, export cost map */
            if (tripCount % 1000 == 0 || tripCount == 10) {
                Tensor all = Tensors.empty();
                ratioMap.values().forEach(s -> all.append(s));
                try {
                    SaveFormats.MATHEMATICA.save(all, new File("/home/clruch/Downloads/"), "diff");
                    // Export.of(new File("/home/clruch/Downloads/diff.mathematica"), all);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean isBelowTolerance(Scalar costMid, Scalar tolerance) {
        Objects.requireNonNull(costMid);
        Objects.requireNonNull(tolerance);
        Scalar deviation = costMid.subtract(RealScalar.ONE);
        return Scalars.lessEquals(deviation.abs(), tolerance);
    }
}
