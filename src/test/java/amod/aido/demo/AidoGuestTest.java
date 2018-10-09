/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.StringScalar;
import junit.framework.TestCase;

public class AidoGuestTest extends TestCase {
    public void testSimple() {
        Tensor config = Tensors.of( //
                StringScalar.of(AidoGuest.SCENARIO), // scenario name
                RealScalar.of(AidoGuest.REQUEST_NUMBER_DESIRED), // ratio of population
                RealScalar.of(AidoGuest.NUMBER_OF_VEHICLES)); // number of vehicles
        assertEquals(config.toString().indexOf('\"'), -1);
    }
}
