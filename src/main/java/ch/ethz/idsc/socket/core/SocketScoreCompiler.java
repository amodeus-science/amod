/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.util.Collection;
import java.util.List;

import ch.ethz.idsc.amodeus.dispatcher.core.RequestStatus;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.SimulationObjectCompiler;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.matsim.av.passenger.AVRequest;

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

    public Tensor compile(long timeMatsim, List<RoboTaxi> roboTaxis, Collection<AVRequest> requests) {
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
