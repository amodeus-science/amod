/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.util.Objects;

import amod.aido.core.AidoDispatcherHost;
import amod.aido.core.AidoScoreElement;
import amod.aido.core.ScoreParameters;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.matsim.xml.XmlDispatcherChanger;
import ch.ethz.idsc.amodeus.matsim.xml.XmlNumberOfVehiclesChanger;
import ch.ethz.idsc.amodeus.prep.LegCount;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.net.StringServerSocket;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.amodeus.video.VideoGenerator;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
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
    private static final String ENV_SCENARIO = "SCENARIO";
    private static final String ENV_FLEET_SIZE = "FLEET_SIZE";
    private static final String ENV_REQUESTS_SIZE = "REQUESTS_SIZE";
    private static final String ENV_VIDEO_EXPORT = "VIDEO_EXPORT";

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
            {
                String env = System.getenv(ENV_SCENARIO);
                if (Objects.nonNull(env))
                    env = scenarioTag;
            }

            /** download the chosen scenario */
            try {
                AidoScenarioResource.extract(scenarioTag);
            } catch (Exception exception) {
                /** send empty tensor "{}" to stop */
                stringSocket.writeln(Tensors.empty());
                /** send fictitious costs */
                stringSocket.writeln(StaticHelper.FAILURE_SCORE);
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

            Scalar nominalFleetSize = Round.of(numReq.multiply(ScoreParameters.GLOBAL.gamma));
            Tensor initialInfo = Tensors.of(numReq, preparer.getBoundingBox(), nominalFleetSize);

            /** send initial data: {numberRequests,boundingBox} */
            stringSocket.writeln(initialInfo);

            /** get additional information */
            String readLine2 = stringSocket.readLine();
            Tensor config2 = Tensors.fromString(readLine2);
            int numReqDes = config2.Get(0).number().intValue();
            int fleetSize = config2.Get(1).number().intValue();

            {
                String env = System.getenv(ENV_REQUESTS_SIZE);
                if (Objects.nonNull(env))
                    try {
                        numReqDes = Integer.parseInt(env);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
            {
                String env = System.getenv(ENV_FLEET_SIZE);
                if (Objects.nonNull(env))
                    try {
                        fleetSize = Integer.parseInt(env);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }

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
                    aidoServer.getOutputDirectory(), preparer.getDatabase());
            AidoScoreElement aidoScoreElement = new AidoScoreElement(fleetSize, numReqDes, preparer.getDatabase());
            analysis.addAnalysisElement(aidoScoreElement);

            AidoExport aidoExport = new AidoExport(aidoScoreElement);
            analysis.addAnalysisExport(aidoExport);

            AidoHtmlReport aidoHtmlReport = new AidoHtmlReport(aidoScoreElement);
            analysis.addHtmlElement(aidoHtmlReport);
            analysis.run();

            { /** create a video if environment variable is set */
                String env = System.getenv(ENV_VIDEO_EXPORT);
                if (Objects.nonNull(env) && env.equalsIgnoreCase("true")) {
                    new VideoGenerator(workingDirectory).start();
                }
            }

            /** send final score,
             * {total waiting time, total distance with customer, total empty distance} */
            stringSocket.writeln(Total.of(aidoScoreElement.getScoreDiffHistory()));

        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }
    }
}
