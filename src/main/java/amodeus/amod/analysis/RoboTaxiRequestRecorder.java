/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amod.analysis;

import java.util.HashMap;
import java.util.Map;

import amodeus.amodeus.analysis.Analysis;
import amodeus.amodeus.analysis.element.AnalysisElement;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;
import amodeus.amodeus.net.SimulationObject;
import amodeus.amodeus.net.VehicleContainerUtils;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;

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
            simulationObject.vehicles.forEach(vc -> vehicleRequestCount.put(vc.vehicleIndex, 0));
            simulationObject.vehicles.forEach(vc -> prevStatus.put(vc.vehicleIndex, RoboTaxiStatus.STAY));
        }

        /** whenever status changes to DriveWithCustomer, the taxi has served a request */
        simulationObject.vehicles.forEach(vc -> //
        {
            int vehicle = vc.vehicleIndex;
            if (VehicleContainerUtils.finalStatus(vc).equals(RoboTaxiStatus.DRIVEWITHCUSTOMER) //
                    && !prevStatus.get(vehicle).equals(RoboTaxiStatus.DRIVEWITHCUSTOMER))
                vehicleRequestCount.put(vehicle, vehicleRequestCount.get(vehicle) + 1);
        });

        /** add previous status */
        simulationObject.vehicles.forEach(vc -> prevStatus.put(vc.vehicleIndex, VehicleContainerUtils.finalStatus(vc)));
    }

    /** @return {@link Tensor} containing the requests served for each {@link RoboTaxi} */
    public Tensor getRequestsPerRoboTaxi() {
        return Tensor.of(vehicleRequestCount.values().stream().map(RealScalar::of));
        // Tensor requests = Tensors.empty();
        // vehicleRequestCount.values().stream().forEach(count -> requests.append(RealScalar.of(count)));
        // return requests;
    }
}
