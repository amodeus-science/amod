/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;

import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.ShortestDurationCalculator;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;

/* package */ class LSDataStep {

    // --
    private final Map<TaxiTrip, Scalar> diffMap = new HashMap<>();
    /** These diff values should converge to 1 */
    public final Scalar diffStart;
    public Scalar diffMid = RealScalar.of(100); // any high enough number ok as initialization
    public final Scalar diffEnd;
    private final Scalar tolerance;
    private final Network network;
    private final LinkSpeedDataContainer lsData;
    private final MatsimAmodeusDatabase db;
    /** this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is adviced o chose slow. No changes
     * are applied for epsilon == 0. */
    private final Scalar epsilon;
    private final Random random;
    private final int dt;

    public LSDataStep(Network network, MatsimAmodeusDatabase db, //
            LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
            int maxIter, Scalar tol, Scalar epsilon, Random random, int dt) {
        this.tolerance = tol;
        this.network = network;
        this.lsData = lsData;
        this.db = db;
        this.epsilon = epsilon;
        this.random = random;
        this.dt = dt;

        diffStart = getDiff();
        System.out.println("diffStart: " + diffStart);

        int iterations = 0;

        while (iterations < maxIter && !isBelowTolerance()) {
            ++iterations;
            tripIteration(allTrips);
        }

        diffEnd = getDiff();
        System.out.println("diffEnd: " + diffEnd);

    }

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
            diffMap.put(trip, comp.nwPathDurationRatio);
            Scalar pathDurationratio = comp.nwPathDurationRatio;
            /** rescale factor such that epsilon in [0,1] maps to [f,1] */
            Scalar rescaleFactor = RealScalar.ONE.subtract(//
                    (RealScalar.ONE.subtract(pathDurationratio)).multiply(epsilon)//
            );

            /** rescale all links */
            ApplyScaling.to(lsData, trip, comp.path, rescaleFactor, dt);

            /** assess every 20 trips if ok */
            if (tripCount % 20 == 0) {
                diffMid = getDiff();
                System.out.println("trip: " + tripCount + " / " + allTrips.size());
                System.out.println("diffMid: " + diffMid);
                if (isBelowTolerance())
                    break;
            }
        }
    }

    private Scalar getDiff() {
        Tensor diffAll = Tensors.empty();
        diffMap.values().forEach(s -> diffAll.append(s));
        if (diffAll.length() == 0)
            return RealScalar.of(-1);
        return (Scalar) Mean.of(diffAll);
    }

    private boolean isBelowTolerance() {
        Objects.requireNonNull(diffMid);
        Objects.requireNonNull(tolerance);
        Scalar deviation = diffMid.subtract(RealScalar.ONE);
        return Scalars.lessEquals(deviation.abs(), tolerance);
    }
}
