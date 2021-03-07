/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod.ext;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Unit;

/* package */ enum UserReferenceFrames implements ReferenceFrame {
    IDENTITY( //
            new IdentityTransformation(), //
            new IdentityTransformation()), //
    SANFRANCISCO( //
            new GeotoolsTransformation("EPSG:26743", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:26743"), //
            Unit.of("ft")), //
    BERLIN( //
            new GeotoolsTransformation("EPSG:31468", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:31468")), //
    SANTIAGO_DE_CHILE( //
            new GeotoolsTransformation("EPSG:32719", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:32719")), //
    AUCKLAND( //
            new GeotoolsTransformation("EPSG:2193", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:2193")), //
    TEL_AVIV( //
            new GeotoolsTransformation("EPSG:2039", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:2039")), //
    CHICAGO( //
            new GeotoolsTransformation("EPSG:3435", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:3435"))
    ;
    // ---
    private final CoordinateTransformation coords_toWGS84;
    private final CoordinateTransformation coords_fromWGS84;
    private final Unit unit;

    UserReferenceFrames(CoordinateTransformation c1, CoordinateTransformation c2) {
        this(c1, c2, SI.METER);
    }

    UserReferenceFrames(CoordinateTransformation c1, CoordinateTransformation c2, Unit unit) {
        coords_toWGS84 = c1;
        coords_fromWGS84 = c2;
        this.unit = unit;
    }

    @Override
    public CoordinateTransformation coords_fromWGS84() {
        return coords_fromWGS84;
    }

    @Override
    public CoordinateTransformation coords_toWGS84() {
        return coords_toWGS84;
    }

    @Override
    public Unit unit() {
        return unit;
    }
}
