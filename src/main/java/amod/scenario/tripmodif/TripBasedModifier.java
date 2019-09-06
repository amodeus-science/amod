/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.amodeus.taxitrip.ExportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.ImportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

public class TripBasedModifier implements TaxiDataModifier {

    private final List<TripModifier> modifiers = new ArrayList<>();

    protected void addModifier(TripModifier modifier) {
        if (Objects.nonNull(modifier))
            modifiers.add(modifier);
    }

    @Override
    public File modify(File taxiData) throws IOException {

        /** gather all original trips */
        List<TaxiTrip> originals = new ArrayList<>();
        ImportTaxiTrips.fromFile(taxiData).forEach(tt -> originals.add(tt));

        /** notify about all the taxi trips */
        originals.forEach(tt -> {
            for (TripModifier modifier : modifiers) {
                modifier.notify(tt);
            }
        });

        /** let modifiers do modifications on each trip, then return */
        List<TaxiTrip> modified = new ArrayList<>();
        originals.forEach(orig -> {
            TaxiTrip changed = orig;
            for (TripModifier modifier : modifiers) {
                changed = modifier.modify(changed);
            }
            modified.add(changed);
        });
        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_modified.csv"));
        ExportTaxiTrips.toFile(modified.stream(), outFile);
        return outFile;
    }

}
