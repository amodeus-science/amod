/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.net.Socket;

import ch.ethz.idsc.amodeus.aido.StringClientSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class AidoGuest {

    public static void main(String[] args) throws Exception {

        /** connect to AidoGuest */
        StringClientSocket clientSocket = new StringClientSocket(new Socket("localhost", 9382)); // TODO

        /** send initial command */
        Tensor config = Tensors.fromString("{SanFrancisco}");
        config.append(RealScalar.of(0.4));
        config.append(RealScalar.of(177));
        clientSocket.write(config.toString() + "\n");
        System.out.println("AidoGuest config: " + config);

        /** receive initial information */
        Tensor initialInfo = Tensors.fromString(clientSocket.reader.readLine());
        System.out.println("AidoGuest initialInfo: " + initialInfo);
        Tensor minX = initialInfo.get(0);
        Tensor minY = initialInfo.get(1);

        /** receive dispatching status and send dispatching command */
        while (true) { // TODO how to exit
            Tensor status = Tensors.fromString(clientSocket.reader.readLine());
            System.out.println("AidoGuest status: " + status);

            // TODO create dispatching command from status
            Tensor command = Tensors.fromString("{{},{}}");
            clientSocket.write(command.toString() + "\n");
            System.out.println("AidoGuest command: " + command);
        }

    }

}
