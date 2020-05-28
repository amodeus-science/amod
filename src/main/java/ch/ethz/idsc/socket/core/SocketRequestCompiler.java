/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket.core;

import java.util.Collection;

import org.matsim.amodeus.dvrp.request.AVRequest;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class SocketRequestCompiler {
    private final MatsimAmodeusDatabase db;

    public SocketRequestCompiler(MatsimAmodeusDatabase db) {
        this.db = db;
    }

    public Tensor compile(Collection<AVRequest> requests) {
        return Tensor.of(requests.stream().map(this::of));
    }

    private Tensor of(AVRequest request) {
        // id
        Tensor info = Tensors.vector(request.getId().index());
        // submission time
        info.append(RealScalar.of(request.getSubmissionTime()));
        // from location
        info.append(TensorCoords.toTensor(db.referenceFrame.coords_toWGS84().transform( //
                request.getFromLink().getCoord())));
        // to location
        info.append(TensorCoords.toTensor(db.referenceFrame.coords_toWGS84().transform( //
                request.getToLink().getCoord())));
        return info;
    }
}
