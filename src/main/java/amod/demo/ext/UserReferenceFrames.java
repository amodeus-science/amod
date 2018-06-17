package amod.demo.ext;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;

public enum UserReferenceFrames implements ReferenceFrame {
	BERLIN( //
			new GeotoolsTransformation("EPSG:31468", "WGS84"), //
			new GeotoolsTransformation("WGS84", "EPSG:31468")), //
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
