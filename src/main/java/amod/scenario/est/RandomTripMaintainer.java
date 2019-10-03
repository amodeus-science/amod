package amod.scenario.est;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ class RandomTripMaintainer {

    /** random generator */
    private final Random random;
    /** maintaining a list of last recorded ratios */
    private final FIFOFixedQueue<Scalar> lastRatios;
    /** number of trips used for convergence assessment */
    private final int checkHorizon;
    /** cost function for convergence check */
    private final Function<List<Scalar>, Scalar> costFunction;
    /** map tracking number of queries */
    private final NavigableMap<Integer, Set<TaxiTrip>> queryMap = new TreeMap<>();
    private final int numTrips;

    public RandomTripMaintainer(List<TaxiTrip> allTrips, int checkHorizon, //
            Function<List<Scalar>, Scalar> costFunction, Random random) {
        lastRatios = new FIFOFixedQueue<>(allTrips.size());
        this.checkHorizon = checkHorizon > allTrips.size() ? allTrips.size() : checkHorizon;
        this.costFunction = costFunction;
        this.random = random;
        numTrips = allTrips.size();

        /** fill query map */
        Collections.shuffle(allTrips, random);
        queryMap.put(0, new HashSet<TaxiTrip>());
        allTrips.stream().forEach(tt -> {
            queryMap.firstEntry().getValue().add(tt);
        });
        GlobalAssert.that(allTrips.size() == queryMap.firstEntry().getValue().size());
    }

    public TaxiTrip nextRandom() {
        // find set of trips with least checks
        int numChecks = 0;
        while (!(queryMap.ceilingEntry(numChecks).getValue().size() > 0))
            ++numChecks;
        Set<TaxiTrip> leastChecked = queryMap.ceilingEntry(numChecks).getValue();

        // select random trip from it
        TaxiTrip selected = leastChecked.stream().skip(random.nextInt(leastChecked.size())).findFirst().get();

        // move to set with +1 checks
        GlobalAssert.that(leastChecked.remove(selected));
        if (queryMap.lastKey() == numChecks) {
            queryMap.put(numChecks + 1, new HashSet<>());
        }
        queryMap.ceilingEntry(numChecks + 1).getValue().add(selected);

        // return
        return selected;
    }

    public void addRecordedRatio(Scalar ratio) {
        lastRatios.manage(ratio);
    }

    public Scalar getRatioCost() {
        return costFunction.apply(lastRatios.getNewest(checkHorizon));
    }
    
    public int numTrips(){
        return numTrips;        
    }
    
    public Tensor getRatios(){
        Tensor ratios = Tensors.empty();
        lastRatios.getNewest(checkHorizon).stream().forEach(s->ratios.append(s));
        return ratios;       
    }
}
