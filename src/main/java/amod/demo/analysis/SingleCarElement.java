/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import ch.ethz.idsc.amodeus.analysis.element.AnalysisElement;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.net.VehicleContainer;

public class SingleCarElement implements AnalysisElement {

    @Override
    public void register(SimulationObject simulationObject) {
        VehicleContainer vc = simulationObject.vehicles.get(1);
        vc.getLinkId();
    }

}
