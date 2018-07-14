/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

import amod.aido.AidoHost;
import ch.ethz.idsc.amodeus.util.net.StringSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.StringScalar;

/** AidoGuest is a simple demo client that interacts with AidoHost.
 * 
 * Usage:
 * java -cp target/amod-VERSION.jar amod.aido.demo.AidoGuest [IP of host] */
public class AidoGuest {

    /** default values for demo */
    private static final String SCENARIO = "TelAviv";
    private static final double POPULATION_RATIO = 0.4;
    private static final int NUMBER_OF_VEHICLES = 177;
    private static final int PRINT_SCORE_PERIOD = 200;

    /** @param args 1 entry which is IP address
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        AidoGuest aidoGuest = new AidoGuest(args.length == 0 ? "localhost" : args[0]);
        aidoGuest.run(SCENARIO, POPULATION_RATIO, NUMBER_OF_VEHICLES);
    }

    // ---
    private final String ip;

    /** @param ip for instance "localhost" */
    public AidoGuest(String ip) {
        this.ip = ip;
    }

    public void run(String scenario, double populationRatio, int numberOfVehicles) throws UnknownHostException, IOException, Exception {
        /** connect to AidoGuest */
        try (StringSocket stringSocket = new StringSocket(new Socket(ip, AidoHost.PORT))) {

            /** send initial command {"SanFrancisco", 0.4, 177} */
            Tensor config = Tensors.of( //
                    StringScalar.of(scenario), // scenario name
                    RealScalar.of(populationRatio), // ratio of population
                    RealScalar.of(numberOfVehicles)); // number of vehicles
            stringSocket.writeln(config);

            final DispatchingLogic dispatchingLogic;
            {
                /** receive initial information */
                Tensor initialInfo = Tensors.fromString(stringSocket.readLine());

                /** the city grid is inside the WGS:84 coordinates bounded by the box
                 * bottomLeft, topRight */
                Tensor bottomLeft = initialInfo.get(0);
                Tensor topRight = initialInfo.get(1);

                /** receive dispatching status and send dispatching command */
                dispatchingLogic = new DispatchingLogic(bottomLeft, topRight);
            }

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

            {
                /** recieve final performance score/stats */
                Tensor finalInfo = Tensors.fromString(stringSocket.readLine());
                // {2712.6744186046512, 0.6200892616333414, 5}
                System.out.println("finalInfo: " + finalInfo);
                // TODO decode numbers in finalInfo and print with interpretation
            }
        } // <- closing string socket
    }

}
