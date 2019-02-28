package amod.demo.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import ch.ethz.idsc.amodeus.analysis.element.AnalysisElement;
import ch.ethz.idsc.amodeus.analysis.element.VehicleStatistic;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.net.VehicleContainer;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;

public class RoboTaxiDistanceRecorder implements AnalysisElement {
    /** link distances in the network must be in [m] */
    private static final Scalar KM2M = RealScalar.of(0.001);
    // ---

    private int simObjIndex = 0; // Index for the Simulation Object which is loaded
    private List<VehicleStatistic> list = new ArrayList<>();
    /** vector for instance {10, 20, ...} */

    /** fields assigned in compile */
    public Tensor totalDistancesPerVehicle;

    /** variable to check for other classes if the consolidatioin already happend */
    public boolean consolidated = false;

    public RoboTaxiDistanceRecorder(int numVehicles, int size, MatsimAmodeusDatabase db) {
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size, db)));
    }

    @Override
    public void register(SimulationObject simulationObject) {

        /** register Simulation Object for distance analysis */
        for (VehicleContainer vehicleContainer : simulationObject.vehicles)
            list.get(vehicleContainer.vehicleIndex).register(simObjIndex, vehicleContainer);

        ++simObjIndex;
    }

    @Override
    public void consolidate() {
        list.forEach(VehicleStatistic::consolidate);


        // total distances driven per vehicle
        totalDistancesPerVehicle = Tensor.of(list.stream().map(vs -> Total.of(vs.distanceTotal))).multiply(KM2M);
        
        consolidated = true;
    }

    /** @return newest distances available {distTotal,distWtCst} */
    public Tensor getNewestDistances() {
        return list.stream() //
                .map(VehicleStatistic::getLatestRecordings) //
                .map(tensor -> tensor.extract(0, 2)) //
                .reduce(Tensor::add) //
                .orElse(Array.zeros(2));
    }

    /** @return An unmodifiable List of all the Vehicle Statistics for all Vehicles in the fleet. */
    public List<VehicleStatistic> getVehicleStatistics() {
        return Collections.unmodifiableList(list);
    }

}