package amod.scenario.tripmodif;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
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
    public File modify(File taxiData, MatsimAmodeusDatabase db) throws IOException {
        List<TaxiTrip> modified = new ArrayList<>();
        ImportTaxiTrips.fromFile(taxiData).forEach(orig -> {
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
