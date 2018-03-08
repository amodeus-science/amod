package amod.demo.ext;

import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.LocationSpecs;

public enum Static {
    ;

    public static void setup() {
        for (LocationSpec locationSpec : UserLocationSpecs.values())
            LocationSpecs.DATABASE.put(locationSpec);
    }

}
