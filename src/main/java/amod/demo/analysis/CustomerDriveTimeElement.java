package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.element.AnalysisElement;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class CustomerDriveTimeElement implements AnalysisElement {

    private Map<Integer, Double> vehicleCustTime = new HashMap<>();
    private Map<Integer, RoboTaxiStatus> prevStatus = new HashMap<>();
    private Map<Integer, Double> prevvehicleTime = new HashMap<>();
    private Map<Integer, Double> vehicleTime = new HashMap<>();

    @Override
    public void register(SimulationObject simulationObject) {
    	
    	double time = (double) simulationObject.now;

        /** initialize map when first simulationObject arrives */
        if (vehicleCustTime.size() == 0) {
            simulationObject.vehicles.stream().forEach(vc -> vehicleCustTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> prevStatus.put(vc.vehicleIndex, RoboTaxiStatus.STAY));
            simulationObject.vehicles.stream().forEach(vc -> prevvehicleTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> vehicleTime.put(vc.vehicleIndex, 0.0));
        }

        simulationObject.vehicles.stream().forEach(vc -> //
        {
            int vehicle = vc.vehicleIndex;
            if (vc.roboTaxiStatus.equals(RoboTaxiStatus.DRIVEWITHCUSTOMER) //
                    && (!prevStatus.get(vehicle).equals(RoboTaxiStatus.DRIVEWITHCUSTOMER))) {
            	prevvehicleTime.put(vehicle, time);
            }
            if ((!vc.roboTaxiStatus.equals(RoboTaxiStatus.DRIVEWITHCUSTOMER)) //
                    && (prevStatus.get(vehicle).equals(RoboTaxiStatus.DRIVEWITHCUSTOMER))) {
            	vehicleTime.put(vehicle, time);
				Double driveTime = vehicleTime.get(vehicle) - prevvehicleTime.get(vehicle);
				GlobalAssert.that(driveTime >= 0);
				vehicleCustTime.put(vehicle, vehicleCustTime.get(vehicle) + driveTime);
            }
        });

        /** add previous status */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            prevStatus.put(vc.vehicleIndex, vc.roboTaxiStatus);

        });

    }

    /** @return {@link Tensor} containing the requests served for each {@link RoboTaxi} */
    public Tensor getCustDriveTimePerRoboTaxi() {
        Tensor waitTimes = Tensors.empty();
        vehicleCustTime.values().stream().forEach(time -> waitTimes.append(RealScalar.of(time)));
        return waitTimes;
    }

}