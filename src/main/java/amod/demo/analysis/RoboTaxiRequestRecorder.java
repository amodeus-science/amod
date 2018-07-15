/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisElement;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** this class records the number of requests served by each {@link RoboTaxi} bases on the
 * {@link SimulationObject} which are loaded during the {@link Analysis} of an AMoDeus
 * simulation run. */
/* package */ class RoboTaxiRequestRecorder implements AnalysisElement {

    private Map<Integer, Integer> vehicleRequestCount = new HashMap<>();
    private Map<Integer, RoboTaxiStatus> prevStatus = new HashMap<>();

    @Override
    public void register(SimulationObject simulationObject) {

        /** initialize map when first simulationObject arrives */
        if (vehicleRequestCount.size() == 0) {
            simulationObject.vehicles.stream().forEach(vc -> vehicleRequestCount.put(vc.vehicleIndex, 0));
            simulationObject.vehicles.stream().forEach(vc -> prevStatus.put(vc.vehicleIndex, RoboTaxiStatus.STAY));
        }

        /** whenever status changes to DriveWithCustomer, the taxi has served a request */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            int vehicle = vc.vehicleIndex;
            if (vc.roboTaxiStatus.equals(RoboTaxiStatus.DRIVEWITHCUSTOMER) //
                    && !prevStatus.get(vehicle).equals(RoboTaxiStatus.DRIVEWITHCUSTOMER))
                vehicleRequestCount.put(vehicle, vehicleRequestCount.get(vehicle) + 1);
        });

        /** add previous status */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            prevStatus.put(vc.vehicleIndex, vc.roboTaxiStatus);

        });

    }

    /** @return {@link Tensor} containing the requests served for each {@link RoboTaxi} */
    public Tensor getRequestsPerRoboTaxi() {
        Tensor requests = Tensors.empty();
        vehicleRequestCount.values().stream().forEach(count -> requests.append(RealScalar.of(count)));
        return requests;
    }

}
