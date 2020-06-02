/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.util.Collection;
import java.util.List;

import amodeus.amodeus.dispatcher.core.RequestStatus;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationObjectCompiler;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import ch.ethz.idsc.tensor.Tensor;

public class SocketScoreCompiler {
    private static final String INFO_LINE = "";
    private static final int TOTAL_MATCHED_REQUESTS = -1;
    // ---
    private final SocketScoreElement socketScoreElement;
    private final MatsimAmodeusDatabase db;

    public SocketScoreCompiler(List<RoboTaxi> roboTaxis, int totReq, MatsimAmodeusDatabase db) {
        this.db = db;
        socketScoreElement = new SocketScoreElement(roboTaxis.size(), totReq, ScoreParameters.GLOBAL, db);
    }

    public Tensor compile(long timeMatsim, List<RoboTaxi> roboTaxis, Collection<PassengerRequest> requests) {
        /** create a {@link SimulationObject} */
        SimulationObjectCompiler simulationObjectCompiler = //
                SimulationObjectCompiler.create(timeMatsim, INFO_LINE, TOTAL_MATCHED_REQUESTS, db);
        simulationObjectCompiler.insertVehicles(roboTaxis);
        simulationObjectCompiler.insertRequests(requests, RequestStatus.EMPTY); // request status not used

        /** insert and evaluate */
        socketScoreElement.register(simulationObjectCompiler.compile());
        return socketScoreElement.getScoreDiff();
    }
}
