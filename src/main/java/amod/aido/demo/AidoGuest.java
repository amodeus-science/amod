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

    /** @param args 1 entry which is IP address
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        AidoGuest aidoGuest = new AidoGuest(args.length == 0 ? "localhost" : args[0]);
        aidoGuest.run("SanFrancisco", 0.4, 177);
    }

    // ---
    private final String address;

    public AidoGuest(String ip) {
        address = ip;
    }

    public void run(String scenario, double populationRatio, int numberOfVehicles) throws UnknownHostException, IOException, Exception {
        /** connect to AidoGuest */
        try (StringSocket clientSocket = new StringSocket(new Socket(address, AidoHost.PORT))) {

            /** send initial command */
            Tensor config = Tensors.empty();
            config.append(StringScalar.of(scenario)); // scenario name
            config.append(RealScalar.of(populationRatio)); // ratio of population
            config.append(RealScalar.of(numberOfVehicles)); // number of vehicles
            clientSocket.writeln(config);

            /** receive initial information */
            Tensor initialInfo = Tensors.fromString(clientSocket.readLine());

            /** the city grid is inside the WGS:84 coordinates bounded by the box
             * bottomLeft, topRight */
            Tensor bottomLeft = initialInfo.get(0);
            Tensor topRight = initialInfo.get(1);

            /** receive dispatching status and send dispatching command */
            DispatchingLogic bdl = new DispatchingLogic(bottomLeft, topRight);

            int count = 0;
            while (true) {
                String string = clientSocket.readLine();
                if (Objects.nonNull(string)) { // when the server
                    Tensor status = Tensors.fromString(string);
                    if (Tensors.isEmpty(status)) // signal to exit
                        break;

                    if (status.length() < 4) {
                        System.out.println("status has unexpected format");
                        System.out.println(status);
                        break;
                    }
                    Tensor score = status.get(3);
                    if (++count % 100 == 0)
                        System.out.println("score = " + score + " at " + status.Get(0));
                    Tensor command = bdl.of(status);
                    clientSocket.writeln(command);
                } else {
                    System.err.println("server terminated prematurely?");
                    break;
                }
            }

            Tensor finalInfo = Tensors.fromString(clientSocket.readLine());
            System.out.println("finalInfo: " + finalInfo);
        }
    }

}
