//code by jph
package amod.demo.ext;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.data.ReferenceFrames;

/* package */ enum UserLocationSpecs implements LocationSpec {
    SANFRANCISCO( //
            ReferenceFrames.SANFRANCISCO, //
            new Coord(-122.4363005, 37.7511686)), // <- no cutting
    ;

    private final ReferenceFrame referenceFrame;
    // increasing the first value goes right
    // increasing the second value goes north
    private final Coord center;

    private UserLocationSpecs(ReferenceFrame referenceFrame, Coord center) {
        this.referenceFrame = referenceFrame;
        this.center = center;
    }

    @Override
    public ReferenceFrame referenceFrame() {
        return referenceFrame;
    }

    @Override
    public Coord center() {
        return center;
    }
}
