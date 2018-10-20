/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.awt.Dimension;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

// TODO might be better off in amodeus later

// tracked fields are of type Tensor/Scalar/String/Boolean
// can assign default values, will be overwritten by values in file
public class ViewerConfig {
    // TODO fields should have default values defined here
    public Tensor coord;
    public Scalar zoom = RealScalar.of(3);
    public Scalar width;
    public Scalar height;
    public Tensor dimensions = Tensors.vector(400, 200);
    public Boolean visibility;
    public String string = "here";
    public Boolean anotherboolean = true;
    // ---

    public Coord getCoord() {
        return TensorCoords.toCoord(coord);
    }

    public Dimension getDimension() {
        return new Dimension( //
                dimensions.Get(0).number().intValue(), //
                dimensions.Get(1).number().intValue());
    }
}
