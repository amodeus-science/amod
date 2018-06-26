/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;

import amod.demo.ScenarioServer;
import ch.ethz.idsc.amodeus.aido.StringClientSocket;
import ch.ethz.idsc.amodeus.aido.StringServerSocket;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class AidoHost {
    public static void main(String[] args) throws Exception {

        /** open String server and wait for initial command */
        StringServerSocket serverSocket = new StringServerSocket(9382, StringClientSocket::new);
        StringClientSocket clientSocket = serverSocket.getSocketWait();
        String readLine = clientSocket.reader.readLine(); // TODO reader private
        Tensor config = Tensors.fromString(readLine);

        String scenarioTag = config.Get(0).toString();
        double populRed = config.Get(1).number().doubleValue();
        int fleetSize = config.Get(2).number().intValue();

        /** download the chosen scenario */
        AidoScenarioDownload.download(scenarioTag);

        /** scenario preparer */        
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        Tensor initialInfo = AidoPreparer.run(workingDirectory, populRed);
        
        /** send initial data (bounding box) */
        clientSocket.write(initialInfo.toString());

        /** run with AIDO dispatcher */
        StaticHelper.changeDispatcherTo("AidoDispatcherHost", workingDirectory);
        StaticHelper.changeVehicleNumberTo(fleetSize, workingDirectory);        
        ScenarioServer.simulate();

        /** run with AIDO dispatcher */
        clientSocket.write(RealScalar.ZERO.toString()); // TODO something useful

    }

}
