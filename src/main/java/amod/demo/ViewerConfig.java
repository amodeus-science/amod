/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.io.TensorProperties;
import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

// TODO might be better off in amodeus later

public class ViewerConfig {
    private MatsimAmodeusDatabase db;

    public Tensor coord = Tensors.empty();
    public Scalar zoom = RealScalar.of(12);
    public Tensor dimensions = Tensors.vector(900, 900);
    public Boolean visibility = true;
    // public String string = "here";

    private ViewerConfig(MatsimAmodeusDatabase db) {
        this.db = db;
    }

    public static ViewerConfig from(MatsimAmodeusDatabase db, File workingDirectory) throws IOException {
        ViewerConfig viewerConfig = new ViewerConfig(db);
        File configFile = new File(workingDirectory, "viewer.properties");
        if (configFile.exists())
            viewerConfig = TensorProperties.wrap(viewerConfig).set(Import.properties(configFile));
        return viewerConfig;
    }

    public Coord getCoord() {
        if (Tensors.isEmpty(this.coord)) {
            return db.getCenter();
        }
        return TensorCoords.toCoord(coord);
    }

    public Dimension getDimension() {
        return new Dimension( //
                dimensions.Get(0).number().intValue(), //
                dimensions.Get(1).number().intValue());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":\n\t" + Stream.of(this.getClass().getFields()).map(f -> {
            Object value;
            try {
                switch (f.getName()) {
                    case "coord":
                        value = this.getCoord();
                        break;
                    case "dimensions":
                        value = this.getDimension();
                        break;
                    default:
                        value = f.get(this);
                        break;
                }
            } catch (IllegalAccessException e) { value = "N/A"; }
            return f.getName() + " = " + value;
        }).collect(Collectors.joining("\n\t"));
    }
}
