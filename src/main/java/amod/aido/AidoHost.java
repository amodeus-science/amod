/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.util.Properties;

import ch.ethz.idsc.amodeus.aido.AidoDispatcherHost;
import ch.ethz.idsc.amodeus.aido.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.matsim.xml.XmlDispatcherChanger;
import ch.ethz.idsc.amodeus.matsim.xml.XmlNumberOfVehiclesChanger;
import ch.ethz.idsc.amodeus.prep.LegCount;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.net.StringServerSocket;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;

/** host that runs in container.
 * a client can connect to a running host via TCP/IP
 * 
 * Usage:
 * java -cp target/amod-VERSION.jar amod.aido.AidoHost [city] */
public enum AidoHost {
    ;
    public static final int PORT = 9382;
    private static final Properties scoreparam = ResourceData.properties("/aido/scoreparam.properties");

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

            /** download the chosen scenario */
            try {
                AidoScenarioDownload2.download(scenarioTag);
            } catch (Exception exception) {
                /** send empty tensor "{}" to stop */
                stringSocket.writeln(Tensors.empty());
                /** send fictitious costs */
                stringSocket.writeln("{-Infinity, -Infinity, -Infinity}");
                throw exception;
            }

            /** setup environment variables */
            StaticHelper.setup();

            /** scenario preparer */
            File workingDirectory = MultiFileTools.getWorkingDirectory();
            System.out.println("Using scenario directory: " + workingDirectory);

            /** run first part of preparer */
            AidoPreparer preparer = new AidoPreparer(workingDirectory);

            /** get number of requests in population */
            Scalar numReq = LegCount.of(preparer.getPopulation(), "av");
            Scalar nominalFleetSize = //
                    Round.of(numReq.multiply(RealScalar.of(Double.parseDouble(scoreparam.getProperty("gamma")))));
            Tensor initialInfo = Tensors.of(numReq, preparer.getBoundingBox(), nominalFleetSize);

            /** send initial data: {numberRequests,boundingBox} */
            stringSocket.writeln(initialInfo);

            /** get additional information */
            String readLine2 = stringSocket.readLine();
            Tensor config2 = Tensors.fromString(readLine2);
            int numReqDes = config2.Get(0).number().intValue();
            int fleetSize = config2.Get(1).number().intValue();

            /** run second part of preparer */
            preparer.run2(numReqDes);

            /** run with AIDO dispatcher */
            XmlDispatcherChanger.of(workingDirectory, AidoDispatcherHost.class.getSimpleName());
            XmlNumberOfVehiclesChanger.of(workingDirectory, fleetSize);
            AidoServer aidoServer = new AidoServer();
            aidoServer.simulate(stringSocket, numReqDes);

            /** send empty tensor "{}" to stop */
            stringSocket.writeln(Tensors.empty());

            /** analyze and send final score */
            Analysis analysis = Analysis.setup(workingDirectory, aidoServer.getConfigFile(), //
                    aidoServer.getOutputDirectory());
            AidoScoreElement aidoScoreElement = new AidoScoreElement(fleetSize);
            analysis.addAnalysisElement(aidoScoreElement);

            AidoExport aidoExport = new AidoExport(aidoScoreElement);
            analysis.addAnalysisExport(aidoExport);

            AidoHtmlReport aidoHtmlReport = new AidoHtmlReport(aidoScoreElement);
            analysis.addHtmlElement(aidoHtmlReport);
            analysis.run();

            /** send final score,
             * {total waiting time, total distance with customer, total empty distance} */
            stringSocket.writeln(Total.of(aidoScoreElement.getScoreDiffHistory()));

        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }
    }
}
