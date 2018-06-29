/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;

import ch.ethz.idsc.amodeus.aido.AidoDispatcherHost;
import ch.ethz.idsc.amodeus.aido.StringServerSocket;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public enum AidoHost {
    ;

    public static void main(String[] args) throws Exception {

        /** open String server and wait for initial command */
        try (StringServerSocket serverSocket = new StringServerSocket(9382)) {
            AidoDispatcherHost.Factory.stringSocket = serverSocket.getSocketWait();
            String readLine = AidoDispatcherHost.Factory.stringSocket.readLine();
            Tensor config = Tensors.fromString(readLine);
            System.out.println("AidoHost config: " + config);
            // Thread.sleep(3000);

            String scenarioTag = config.Get(0).toString();
            double populRed = config.Get(1).number().doubleValue();
            int fleetSize = config.Get(2).number().intValue();

            /** download the chosen scenario */
            AidoScenarioDownload.download(scenarioTag);

            /** scenario preparer */
            File workingDirectory = MultiFileTools.getWorkingDirectory();
            Tensor initialInfo = AidoPreparer.run(workingDirectory, populRed);

            /** send initial data (bounding box) */
            AidoDispatcherHost.Factory.stringSocket.writeln(initialInfo);

            /** run with AIDO dispatcher */
            StaticHelper.changeDispatcherTo("AidoDispatcherHost", workingDirectory);
            StaticHelper.changeVehicleNumberTo(fleetSize, workingDirectory);
            AidoServer.simulate();

            /** run with AIDO dispatcher */
            AidoDispatcherHost.Factory.stringSocket.writeln(Tensors.empty());
            AidoDispatcherHost.Factory.stringSocket.writeln(RealScalar.ZERO); // TODO something useful
        }
    }

}
