/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod.ext;

import ch.ethz.idsc.amodtaxi.scenario.chicago.ChicagoReferenceFrames;
import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;

/* package */ enum UserLocationSpecs implements LocationSpec {
    SANFRANCISCO( //
            UserReferenceFrames.SANFRANCISCO, //
            new Coord(-122.4363005, 37.7511686)), //
    BERLIN( //
            UserReferenceFrames.BERLIN, //
            new Coord(4595438.15, 5821747.77)), //
    SANTIAGO_DE_CHILE( //
            UserReferenceFrames.SANTIAGO_DE_CHILE, //
            new Coord(-3956418.76, -7864204.17)), //
    AUCKLAND( //
            UserReferenceFrames.AUCKLAND, //
            new Coord(-36.8426, 174.7662)), //
    TEL_AVIV( //
            UserReferenceFrames.TEL_AVIV, //
            new Coord(179549.58, 665848.14)), //
    CHICAGO( //
            ChicagoReferenceFrames.CHICAGO, //
            new Coord(-74.005, 40.712))
    ;

    private final ReferenceFrame referenceFrame;
    // increasing the first value goes right
    // increasing the second value goes north
    private final Coord center;

    UserLocationSpecs(ReferenceFrame referenceFrame, Coord center) {
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
