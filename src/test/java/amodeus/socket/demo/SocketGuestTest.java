/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket.demo;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.StringScalar;
import junit.framework.TestCase;

public class SocketGuestTest extends TestCase {
    public void testSimple() {
        Tensor config = Tensors.of( //
                StringScalar.of(SocketGuest.SCENARIO), // scenario name
                RealScalar.of(SocketGuest.REQUEST_NUMBER_DESIRED), // ratio of population
                RealScalar.of(SocketGuest.NUMBER_OF_VEHICLES)); // number of vehicles
        assertEquals(config.toString().indexOf('\"'), -1);
    }
}
