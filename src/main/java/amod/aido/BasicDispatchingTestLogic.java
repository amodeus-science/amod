/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.sca.Round;

/* package */ class BasicDispatchingTestLogic {

    private final Set<Scalar> matchedReq = new HashSet<>();

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

            /** for each unassinged request, add a taxi in STAY mode */
            for (Tensor request : requests.values()) {
                if (!matchedReq.contains(request.Get(0))) {
                    while (index < status.get(1).length()) {
                        Tensor roboTaxi = status.get(1, index);
                        if (RoboTaxiStatus.valueOf(roboTaxi.Get(2).toString())//
                                .equals(RoboTaxiStatus.STAY)) {
                            pickup.append(Tensors.of(roboTaxi.Get(0), request.Get(0)));
                            matchedReq.add(request.Get(0));
                            ++index;
                            break;
                        }
                        ++index;
                    }
                }
            }
        }
        return Tensors.of(pickup, rebalance);
    }
}
