/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket;

import java.io.File;
import java.util.Objects;

import org.matsim.amodeus.config.AmodeusModeConfig;

import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.matsim.xml.ConfigDispatcherChanger;
import ch.ethz.idsc.amodeus.matsim.xml.ConfigVehiclesChanger;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.prep.LegCount;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.net.StringServerSocket;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.amodeus.video.VideoGenerator;
import ch.ethz.idsc.socket.core.ScoreParameters;
import ch.ethz.idsc.socket.core.SocketDispatcherHost;
import ch.ethz.idsc.socket.core.SocketScoreElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;

//TODO refactor and shorten @clruch
/** host that runs in container.
 * a client can connect to a running host via TCP/IP
 * 
 * Usage:
 * java -cp target/amod-VERSION.jar amod.socket.SocketHost [city] */
public enum SocketHost {
    ;
    public static final int PORT = 9382;
    private static final String ENV_SCENARIO = "SCENARIO";
    private static final String ENV_FLEET_SIZE = "FLEET_SIZE";
    private static final String ENV_REQUESTS_SIZE = "REQUESTS_SIZE";
    private static final String ENV_VIDEO_EXPORT = "VIDEO_EXPORT";

    public static void main(String[] args) throws Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        run(workingDirectory);
    }

    public static void run(File workingDirectory) throws Exception {
        System.out.println("Using scenario directory: " + workingDirectory);
        StringSocket stringSocket = null;

        /** open String server and wait for initial command */
        try (StringServerSocket serverSocket = new StringServerSocket(PORT)) {
            stringSocket = serverSocket.getSocketWait();
            serverSocket.close(); // only allow one connection
            // ---
            String readLine = stringSocket.readLine();
            Tensor config = Tensors.fromString(readLine);
            System.out.println("SocketHost config: " + config);
            Thread.sleep(1000);
            String scenarioTag = config.Get(0).toString();
            {
                String env = System.getenv(ENV_SCENARIO);
                if (Objects.nonNull(env))
                    env = scenarioTag;
            }

            /** download the chosen scenario */
            SocketScenarioResource.extract(scenarioTag, workingDirectory);

            /** setup environment variables */
            StaticHelper.setup();

            /** run first part of scenario preparer */
            SocketPreparer preparer = new SocketPreparer(workingDirectory);

            /** get number of requests in population */
            long numReq = LegCount.of(preparer.getPopulation(), AmodeusModeConfig.DEFAULT_MODE);

            Scalar nominalFleetSize = Round.of(RealScalar.of(numReq).multiply(ScoreParameters.GLOBAL.gamma));
            Tensor initialInfo = Tensors.of(RealScalar.of(numReq), preparer.getBoundingBox(), nominalFleetSize);

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
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
            }
            {
                String env = System.getenv(ENV_FLEET_SIZE);
                if (Objects.nonNull(env))
                    try {
                        fleetSize = Integer.parseInt(env);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
            }

            /** run second part of preparer */
            preparer.run2(numReqDes);

            /** run with socket dispatcher */
            ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
            String simConfigPath = scenarioOptions.getSimulationConfigName();
            ConfigDispatcherChanger.change(simConfigPath, SocketDispatcherHost.class.getSimpleName());
            ConfigVehiclesChanger.change(simConfigPath, fleetSize);
            SocketServer socketServer = new SocketServer();
            socketServer.simulate(stringSocket, numReqDes, workingDirectory);

            /** send empty tensor "{}" to stop */
            stringSocket.writeln(Tensors.empty());

            /** analyze and send final score */
            Analysis analysis = Analysis.setup(socketServer.getScenarioOptions(), socketServer.getOutputDirectory(), //
                    socketServer.getNetwork(), preparer.getDatabase());
            SocketScoreElement socketScoreElement = new SocketScoreElement(fleetSize, numReqDes, preparer.getDatabase());
            analysis.addAnalysisElement(socketScoreElement);

            SocketExport socketExport = new SocketExport(socketScoreElement);
            analysis.addAnalysisExport(socketExport);

            SocketHtmlReport socketHtmlReport = new SocketHtmlReport(socketScoreElement);
            analysis.addHtmlElement(socketHtmlReport);
            analysis.run();

            { /** create a video if environment variable is set */
                String env = System.getenv(ENV_VIDEO_EXPORT);
                if (Objects.nonNull(env) && env.equalsIgnoreCase("true"))
                    new VideoGenerator(workingDirectory).start();
            }

            /** send final score,
             * {total waiting time, total distance with customer, total empty distance} */
            stringSocket.writeln(Total.of(socketScoreElement.getScoreDiffHistory()));

        } catch (Exception exception) {
            // exception.printStackTrace();
            shutdown(stringSocket);
            throw exception;
        }
    }

    private static void shutdown(StringSocket stringSocket) throws Exception {
        if (Objects.nonNull(stringSocket)) {
            /** send empty tensor "{}" to stop */
            stringSocket.writeln(Tensors.empty());
            /** send fictitious costs */
            stringSocket.writeln(StaticHelper.FAILURE_SCORE);
        }
    }
}
