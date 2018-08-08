/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;

import ch.ethz.idsc.amodeus.aido.AidoDispatcherHost;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.matsim.xml.XmlDispatcherChanger;
import ch.ethz.idsc.amodeus.matsim.xml.XmlNumberOfVehiclesChanger;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.net.StringServerSocket;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** host that runs in container.
 * a client can connect to a running host via TCP/IP
 * 
 * Usage:
 * java -cp target/amod-VERSION.jar amod.aido.AidoHost [city] */
public enum AidoHost {
    ;
    public static final int PORT = 9382;

    public static void main(String[] args) throws Exception {
        /** open String server and wait for initial command */
        try (StringServerSocket serverSocket = new StringServerSocket(PORT)) {
            StringSocket stringSocket = serverSocket.getSocketWait();
            serverSocket.close(); // only allow one connection
            // ---
            String readLine = stringSocket.readLine();
            Tensor config = Tensors.fromString(readLine);
            System.out.println("AidoHost config: " + config);
            Thread.sleep(1000);

            String scenarioTag = config.Get(0).toString();
            double populRed = config.Get(1).number().doubleValue();
            int fleetSize = config.Get(2).number().intValue();

            /** download the chosen scenario */
            AidoScenarioDownload.download(scenarioTag);

            /** setup environment variables */
            StaticHelper.setup();

            /** scenario preparer */
            File workingDirectory = MultiFileTools.getWorkingDirectory();
            System.out.println("Using scenario directory: " + workingDirectory);

            Tensor initialInfo = AidoPreparer.run(workingDirectory, populRed);

            /** send initial data (bounding box) */
            stringSocket.writeln(initialInfo);

            /** run with AIDO dispatcher */
            XmlDispatcherChanger.of(workingDirectory, AidoDispatcherHost.class.getSimpleName());
            XmlNumberOfVehiclesChanger.of(workingDirectory, fleetSize);
            AidoServer aidoServer = new AidoServer();
            aidoServer.simulate(stringSocket);

            /** send empty tensor "{}" to stop */
            stringSocket.writeln(Tensors.empty());

            /** analyze and send final score */
            Analysis analysis = Analysis.setup(workingDirectory, aidoServer.getConfigFile(), //
                    aidoServer.getOutputDirectory());
            AidoHtmlReport aidoHtmlReport = new AidoHtmlReport();
            analysis.addHtmlElement(aidoHtmlReport);
            analysis.run();

            /** send final score, currently {mean waiting time, share of empty distance, number of taxis} */
            stringSocket.writeln(aidoHtmlReport.getFinalScore());

        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }
    }
}
