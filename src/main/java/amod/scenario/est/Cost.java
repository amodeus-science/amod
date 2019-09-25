package amod.scenario.est;

import java.util.Map;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Mean;

/** Cost must be positive */
/* package */ enum Cost {
    ;

    public static Scalar mean(Map<TaxiTrip, Scalar> ratioMap) {
        Tensor diffAll = Tensors.empty();
        ratioMap.values().forEach(s -> diffAll.append(s));
        Scalar cost = ((Scalar) Mean.of(diffAll)).subtract(RealScalar.ONE).abs();
        GlobalAssert.that(Scalars.lessEquals(RealScalar.ZERO, cost));
        return cost;
    }

    public static Scalar max(Map<TaxiTrip, Scalar> ratioMap) {
        Scalar cost = ratioMap.values().stream()//
                .map(s -> s.subtract(RealScalar.ONE).abs()).reduce(Max::of).get();
        GlobalAssert.that(Scalars.lessEquals(RealScalar.ZERO, cost));
        return cost;
    }

}
