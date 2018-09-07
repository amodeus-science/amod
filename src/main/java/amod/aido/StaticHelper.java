/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.alg.Array;

/* package */ enum StaticHelper {
    ;
    static final String FAILURE_SCORE = Array.of(l -> DoubleScalar.NEGATIVE_INFINITY, 3).toString();

    static void setup() {
        System.setProperty("matsim.preferLocalDtds", "true");
    }
}
