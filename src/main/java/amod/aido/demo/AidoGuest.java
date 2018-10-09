/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

import amod.aido.AidoHost;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.StringScalar;

/** AidoGuest is a simple demo client that interacts with AidoHost.
 * 
 * Usage:
 * java -cp target/amod-VERSION.jar amod.aido.demo.AidoGuest [IP of host] */
public class AidoGuest {

    /** default values for demo */
    static final String SCENARIO = "SanFrancisco.20080518";
    static final int REQUEST_NUMBER_DESIRED = 500;
    static final int NUMBER_OF_VEHICLES = 20;
    private static final int PRINT_SCORE_PERIOD = 200;

    /** @param args 1 entry which is IP address
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        AidoGuest aidoGuest = new AidoGuest(args.length == 0 ? "localhost" : args[0]);
        aidoGuest.run(SCENARIO, REQUEST_NUMBER_DESIRED, NUMBER_OF_VEHICLES);
    }

    // ---
    private final String ip;

    /** @param ip for instance "localhost" */
    public AidoGuest(String ip) {
        this.ip = ip;
    }

    public void run(String scenario, int requestsDesired, int numberOfVehicles) throws UnknownHostException, IOException, Exception {
        /** connect to AidoGuest */
        try (StringSocket stringSocket = new StringSocket(new Socket(ip, AidoHost.PORT))) {

            /** send initial command, e.g., {SanFrancisco.20080518} */
            Tensor config = Tensors.of(StringScalar.of(scenario)); /** scenario name */
            stringSocket.writeln(config);

            /** receive information on chosen scenario, i.e., bounding box and number of
             * requests, the city grid is inside the WGS:84 coordinates bounded by the
             * box bottomLeft, topRight,
             * {{longitude min, latitude min}, {longitude max, latitude max}} */
            Tensor scenarioInfo = Tensors.fromString(stringSocket.readLine());
            Scalar numReq = (Scalar) scenarioInfo.get(0);
            Tensor bbox = scenarioInfo.get(1);
            Tensor bottomLeft = bbox.get(0);
            Tensor topRight = bbox.get(1);
            Scalar nominalFleetSize = (Scalar) scenarioInfo.get(2);

            /** chose number of Requests and fleet size */
            Scalar numReqDes = RealScalar.of(requestsDesired);
            GlobalAssert.that(Scalars.lessEquals(numReqDes, numReq));
            Scalar numRoboTaxi = RealScalar.of(numberOfVehicles);

            System.out.println("Nominal fleet size: " + nominalFleetSize);
            System.out.println("Chosen fleet size:  " + numRoboTaxi);

            Tensor configSize = Tensors.of(numReqDes, numRoboTaxi);
            stringSocket.writeln(configSize);

            final DispatchingLogic dispatchingLogic;

            /** receive dispatching status and send dispatching command */
            dispatchingLogic = new DispatchingLogic(bottomLeft, topRight);

            int count = 0;
            while (true) {
                String string = stringSocket.readLine();
                if (Objects.nonNull(string)) { // when the server closed prematurely
                    Tensor status = Tensors.fromString(string);
                    if (Tensors.isEmpty(status)) // server signal that simulation is finished
                        break;

                    Tensor score = status.get(3);
                    if (++count % PRINT_SCORE_PERIOD == 0)
                        System.out.println("score = " + score + " at " + status.Get(0));

                    Tensor command = dispatchingLogic.of(status);
                    stringSocket.writeln(command);
                } else {
                    System.err.println("server terminated prematurely?");
                    break;
                }
            }

            /** recieve final performance score/stats */
            Tensor finalScores = Tensors.fromString(stringSocket.readLine());
            System.out.println("final service quality score:  " + finalScores.Get(1));
            System.out.println("final efficiency score:       " + finalScores.Get(2));
            System.out.println("final fleet size score:       " + finalScores.Get(3));

        } // <- closing string socket
    }
}
