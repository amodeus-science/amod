package amod.scenario.est;

import java.util.Map;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Mean;

/* package */ enum Cost {
    ;

    public static Scalar mean(Map<TaxiTrip, Scalar> ratioMap) {
        Tensor diffAll = Tensors.empty();
        ratioMap.values().forEach(s -> diffAll.append(s));
        return (Scalar) Mean.of(diffAll);
    }

    public static Scalar max(Map<TaxiTrip, Scalar> ratioMap) {
        Scalar min = ratioMap.values().stream()//
                .map(s -> s.subtract(RealScalar.ONE).abs()).reduce(Max::of).get();
        return min;
    }

}
