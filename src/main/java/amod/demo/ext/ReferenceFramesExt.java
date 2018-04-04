package amod.demo.ext;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;

public enum ReferenceFramesExt implements ReferenceFrame {
    SIOUXFALLS( //
            new SiouxFallstoWGS84(), //
            new WGS84toSiouxFalls()) //

    ;
    // ---
    private final CoordinateTransformation coords_toWGS84;
    private final CoordinateTransformation coords_fromWGS84;

 private ReferenceFramesExt(CoordinateTransformation c1, CoordinateTransformation c2) {
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

    public static ReferenceFrame fromString(String stringRef) {
        for (ReferenceFrame rframe : ReferenceFramesExt.values()) {
            if (rframe.toString().equals(stringRef))
                return rframe;
        }
        return null;
    }
}