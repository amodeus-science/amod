/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.HashSet;
import java.util.Set;

import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.sca.Round;

/* package */ class BasicDispatchingTestLogic {

    private Set<Scalar> matchedReq = new HashSet();

    public BasicDispatchingTestLogic() {

    }

    public Tensor of(Tensor status) {
        Tensor pickup = Tensors.empty();
        Tensor rebalance = Tensors.empty();

        Scalar time = status.Get(0);
        if (Round.toMultipleOf(RealScalar.of(60)).apply(time).equals(time)) { // every minute

            int index = 0;

            for (Tensor request : status.get(2)) {
                if (!matchedReq.contains(request.Get(0))) {
                    while (index < status.get(1).length()) {

                        Tensor roboTaxi = status.get(1, index);

                        if (RoboTaxiStatus.valueOf(roboTaxi.Get(2).toString()).equals(RoboTaxiStatus.STAY)) {
                            pickup.append(Tensors.of(roboTaxi.Get(0), request.Get(0)));
                            matchedReq.add(request.Get(0));
                            ++index;
                            break;
                        }
                        ++index;

                    }

                }

            }
            // find open requests

            // assing available vehicle to every request

        }

        return Tensors.of(pickup, rebalance);
    }

}
