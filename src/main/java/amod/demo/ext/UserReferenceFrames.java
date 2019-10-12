/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.ext;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;

/* package */ enum UserReferenceFrames implements ReferenceFrame {
    IDENTITY( //
            new IdentityTransformation(), //
            new IdentityTransformation()), //
    SANFRANCISCO( //
            new GeotoolsTransformation("EPSG:26743", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:26743")), //
    BERLIN( //
            new GeotoolsTransformation("EPSG:31468", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:31468")), //
    SANTIAGO_DE_CHILE( //
            new GeotoolsTransformation("EPSG:32719", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:32719")), //
    AUCKLAND( //
            new GeotoolsTransformation("EPSG:3788", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:3788")), //
    TEL_AVIV( //
            new GeotoolsTransformation("EPSG:2039", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:2039")),//
    ;
    // ---
    private final CoordinateTransformation coords_toWGS84;
    private final CoordinateTransformation coords_fromWGS84;

    private UserReferenceFrames(CoordinateTransformation c1, CoordinateTransformation c2) {
        coords_toWGS84 = c1;
        coords_fromWGS84 = c2;
    }

    @Override
    public CoordinateTransformation coords_fromWGS84() {
        return coords_fromWGS84;
    }

    @Override
    public CoordinateTransformation coords_toWGS84() {
        return coords_toWGS84;
    }
}
