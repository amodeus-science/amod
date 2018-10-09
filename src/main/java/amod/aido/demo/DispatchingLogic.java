/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.pdf.Distribution;
import ch.ethz.idsc.tensor.pdf.RandomVariate;
import ch.ethz.idsc.tensor.pdf.UniformDistribution;
import ch.ethz.idsc.tensor.sca.Round;

/** dispatching logic in the AidoGuest demo to compute dispatching instructions
 * that are forwarded to the AidoHost */
/* package */ class DispatchingLogic {

    private final Set<Scalar> matchedReq = new HashSet<>();
    private final Set<Scalar> matchedTax = new HashSet<>();
    private final Distribution dist_lng;
    private final Distribution dist_lat;

    /** @param bottomLeft {lngMin, latMin}
     * @param topRight {lngMax, latMax} */
    public DispatchingLogic(Tensor bottomLeft, Tensor topRight) {
        Scalar lngMin = bottomLeft.Get(0);
        Scalar lngMax = topRight.Get(0);
        Scalar latMin = bottomLeft.Get(1);
        Scalar latMax = topRight.Get(1);

        System.out.println("minimum longitude in network: " + lngMin);
        System.out.println("maximum longitude in network: " + lngMax);
        System.out.println("minimum latitude  in network: " + latMin);
        System.out.println("maximum latitude  in network: " + latMax);

        /** Example:
         * minimum longitude in network: -71.38020297181387
         * maximum longitude in network: -70.44406349551404
         * minimum latitude in network: -33.869660953686626
         * maximum latitude in network: -33.0303523690584 */

        dist_lng = UniformDistribution.of(lngMin, lngMax);
        dist_lat = UniformDistribution.of(latMin, latMax);
    }

    public Tensor of(Tensor status) {
        Tensor pickup = Tensors.empty();
        Tensor rebalance = Tensors.empty();

        Scalar time = status.Get(0);
        if (Round.toMultipleOf(RealScalar.of(60)).apply(time).equals(time)) { // every minute
            int index = 0;

            /** sort requests according to submission time */
            SortedMap<Scalar, Tensor> requests = new TreeMap<>();
            for (Tensor request : status.get(2)) {
                requests.put(request.Get(1), request);
            }

            /** for each unassigned request, add a taxi in STAY mode */
            for (Tensor request : requests.values()) {
                if (!matchedReq.contains(request.Get(0))) {
                    while (index < status.get(1).length()) {
                        Tensor roboTaxi = status.get(1, index);
                        if (RoboTaxiStatus.valueOf(roboTaxi.Get(2).toString())//
                                .equals(RoboTaxiStatus.STAY)) {
                            pickup.append(Tensors.of(roboTaxi.Get(0), request.Get(0)));
                            matchedReq.add(request.Get(0));
                            matchedTax.add(roboTaxi.Get(0));
                            ++index;
                            break;
                        }
                        ++index;
                    }
                }
            }

            /** rebalance 1 of the remaining and unmatched STAY taxis */
            for (int i = 0; i < status.get(1).length(); ++i) {
                Tensor roboTaxi = status.get(1, i);
                if (RoboTaxiStatus.valueOf(roboTaxi.Get(2).toString())//
                        .equals(RoboTaxiStatus.STAY)) {
                    if (!matchedTax.contains(roboTaxi.Get(0))) {
                        Tensor rebalanceLocation = getRandomRebalanceLocation();
                        rebalance.append(Tensors.of(roboTaxi.Get(0), rebalanceLocation));
                        break;
                    }
                }
            }
        }
        return Tensors.of(pickup, rebalance);
    }

    private Tensor getRandomRebalanceLocation() {
        /** ATTENTION: AMoDeus internally uses the convention
         * (longitude, latitude) for a WGS:84 pair,
         * not the other way around as in some other cases. */
        return Tensors.of( //
                RandomVariate.of(dist_lng), //
                RandomVariate.of(dist_lat));
    }
}
