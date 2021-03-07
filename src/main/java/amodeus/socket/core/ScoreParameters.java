/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket.core;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.ref.ObjectProperties;

/** values in class are required by SocketHost
 * therefore class was made public */
public class ScoreParameters {
    /** overrides default values defined in class
     * with the values parsed from the properties file */
    public static final ScoreParameters GLOBAL = ObjectProperties.wrap(new ScoreParameters()) //
            .set(ResourceData.properties("/socket/ScoreParameters.properties"));

    /** service quality */
    public Tensor alpha12 = Tensors.fromString("{-0.5[s^-1], -0.7[m^-1]}");

    /** efficiency score */
    public Tensor alpha34 = Tensors.fromString("{-0.5[s^-1], -0.5[m^-1]}");

    /** standard fleet size as a fraction of the total number of requests */
    public Scalar gamma = RealScalar.of(0.025);

    /** mean wait time for the fleet reduction */
    public Scalar wmean = Quantity.of(300.0, "s");
}
