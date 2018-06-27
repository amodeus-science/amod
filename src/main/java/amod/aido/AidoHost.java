/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;

import ch.ethz.idsc.amodeus.aido.AidoDispatcherHost;
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
        StringClientSocket stringSocket = serverSocket.getSocketWait();
        String readLine = stringSocket.readLine();
        Tensor config = Tensors.fromString(readLine);
        System.out.println("AidoHost config: " + config);
        Thread.sleep(3000);

        String scenarioTag = config.Get(0).toString();
        double populRed = config.Get(1).number().doubleValue();
        int fleetSize = config.Get(2).number().intValue();

        /** download the chosen scenario */
        AidoScenarioDownload.download(scenarioTag);

        /** scenario preparer */
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        Tensor initialInfo = AidoPreparer.run(workingDirectory, populRed);

        /** send initial data (bounding box) */
        stringSocket.writeln(initialInfo);

        /** run with AIDO dispatcher */
        StaticHelper.changeDispatcherTo("AidoDispatcherHost", workingDirectory);
        StaticHelper.changeVehicleNumberTo(fleetSize, workingDirectory);
        AidoServer.simulate(stringSocket);

        /** run with AIDO dispatcher */
        stringSocket.writeln(Tensors.empty());
        stringSocket.writeln(RealScalar.ZERO); // TODO something useful

    }

}
