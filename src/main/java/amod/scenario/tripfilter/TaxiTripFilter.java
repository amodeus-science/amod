/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;

/** Contains a set of filters that process an individual {@link TaxiTrip}
 * and let it pass or not: TaxiTrip -> {true,false} */
public class TaxiTripFilter {
    private final List<Predicate<TaxiTrip>> filters = new ArrayList<>();

    public final void addFilter(Predicate<TaxiTrip> filter) {
        filters.add(filter);
    }

    public final Stream<TaxiTrip> filterStream(Stream<TaxiTrip> stream) {
        System.out.println("Number of filters: " + filters.size());
        for (Predicate<TaxiTrip> dataFilter : filters) {
            System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");
            stream = stream.filter(dataFilter);
        }
        return stream;
    }

}
