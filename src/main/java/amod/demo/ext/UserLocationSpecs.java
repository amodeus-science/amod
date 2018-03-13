//code by jph
package amod.demo.ext;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.data.ReferenceFrames;

/* package */ enum UserLocationSpecs implements LocationSpec {
    SIOUXFALLS_CITY( //
            ReferenceFrames.SIOUXFALLS, //
            new Coord(678365.311581, 4827050.237694)), // <- no cutting
    ZURICH_CITY( //
            ReferenceFrames.SWITZERLAND, //
            new Coord(2683600.0, 1251400.0)), //
    BASEL_CITY( //
            ReferenceFrames.SWITZERLAND, //
            new Coord(2612859.0, 1266343.0)), //
    HOMBURGERTAL( //
            ReferenceFrames.SWITZERLAND, //
            new Coord(2630647.0, 1251120.0)), // radius of 10000
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