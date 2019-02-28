package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.element.AnalysisElement;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxiStatus;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class TimeElement implements AnalysisElement {

    private Map<Integer, Double> vehicleDriveTime = new HashMap<>();
    private Map<Integer, RoboTaxiStatus> prevStatus = new HashMap<>();
    private Map<Integer, Double> prevvehicleTime = new HashMap<>();
    private Map<Integer, Double> vehicleTime = new HashMap<>();

    @Override
    public void register(SimulationObject simulationObject) {
    	
    	double time = (double) simulationObject.now;

        /** initialize map when first simulationObject arrives */
        if (vehicleDriveTime.size() == 0) {
            simulationObject.vehicles.stream().forEach(vc -> vehicleDriveTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> prevStatus.put(vc.vehicleIndex, RoboTaxiStatus.STAY));
            simulationObject.vehicles.stream().forEach(vc -> prevvehicleTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> vehicleTime.put(vc.vehicleIndex, 0.0));
        }

        /** whenever status changes to DriveWithCustomer, the taxi has served a request */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            int vehicle = vc.vehicleIndex;
            if (!vc.roboTaxiStatus.equals(RoboTaxiStatus.STAY) && !vc.roboTaxiStatus.equals(RoboTaxiStatus.WAITING) //
                    && (prevStatus.get(vehicle).equals(RoboTaxiStatus.STAY) || prevStatus.get(vehicle).equals(RoboTaxiStatus.WAITING))) {
            	prevvehicleTime.put(vehicle, time);
            }
            if ((vc.roboTaxiStatus.equals(RoboTaxiStatus.STAY) || vc.roboTaxiStatus.equals(RoboTaxiStatus.WAITING)) //
                    && (!prevStatus.get(vehicle).equals(RoboTaxiStatus.STAY) && !prevStatus.get(vehicle).equals(RoboTaxiStatus.WAITING))) {
            	vehicleTime.put(vehicle, time);
            	Double driveTime = vehicleTime.get(vehicle) - prevvehicleTime.get(vehicle);
            	vehicleDriveTime.put(vehicle, vehicleDriveTime.get(vehicle) + driveTime);
            }
        });

        /** add previous status */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            prevStatus.put(vc.vehicleIndex, vc.roboTaxiStatus);

        });

    }

    /** @return {@link Tensor} containing the requests served for each {@link RoboTaxi} */
    public Tensor getDriveTimePerRoboTaxi() {
        Tensor driveTimes = Tensors.empty();
        vehicleDriveTime.values().stream().forEach(time -> driveTimes.append(RealScalar.of(time)));
        return driveTimes;
    }

}