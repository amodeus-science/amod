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

public class ParkingTimeElement implements AnalysisElement {

    private Map<Integer, Double> vehicleParkTime = new HashMap<>();
    private Map<Integer, RoboTaxiStatus> prevStatus = new HashMap<>();
    private Map<Integer, Double> prevvehicleTime = new HashMap<>();
    private Map<Integer, Double> vehicleTime = new HashMap<>();

    @Override
    public void register(SimulationObject simulationObject) {
    	
    	double time = (double) simulationObject.now;

        /** initialize map when first simulationObject arrives */
        if (vehicleParkTime.size() == 0) {
            simulationObject.vehicles.stream().forEach(vc -> vehicleParkTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> prevStatus.put(vc.vehicleIndex, RoboTaxiStatus.STAY));
            simulationObject.vehicles.stream().forEach(vc -> prevvehicleTime.put(vc.vehicleIndex, 0.0));
            simulationObject.vehicles.stream().forEach(vc -> vehicleTime.put(vc.vehicleIndex, 0.0));
        }

        simulationObject.vehicles.stream().forEach(vc -> //
        {
            int vehicle = vc.vehicleIndex;
            if (vc.roboTaxiStatus.equals(RoboTaxiStatus.PARKING) //
                    && (!prevStatus.get(vehicle).equals(RoboTaxiStatus.PARKING))) {
            	prevvehicleTime.put(vehicle, time);
            }
            if ((!vc.roboTaxiStatus.equals(RoboTaxiStatus.PARKING)) //
                    && (prevStatus.get(vehicle).equals(RoboTaxiStatus.PARKING))) {
            	vehicleTime.put(vehicle, time);
				Double driveTime = vehicleTime.get(vehicle) - prevvehicleTime.get(vehicle);
				GlobalAssert.that(driveTime >= 0);
				vehicleParkTime.put(vehicle, vehicleParkTime.get(vehicle) + driveTime);
            }
        });

        /** add previous status */
        simulationObject.vehicles.stream().forEach(vc -> //
        {
            prevStatus.put(vc.vehicleIndex, vc.roboTaxiStatus);

        });

    }

    /** @return {@link Tensor} containing the requests served for each {@link RoboTaxi} */
    public Tensor getParkingTimePerRoboTaxi() {
        Tensor waitTimes = Tensors.empty();
        vehicleParkTime.values().stream().forEach(time -> waitTimes.append(RealScalar.of(time)));
        return waitTimes;
    }

}