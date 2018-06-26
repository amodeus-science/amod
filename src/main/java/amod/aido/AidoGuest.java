/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.net.Socket;

import ch.ethz.idsc.amodeus.aido.StringClientSocket;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class AidoGuest {

    /** @param args 1 entry which is IP address
     * @throws Exception */
    public static void main(String[] args) throws Exception {

        /** connect to AidoGuest */
        String address = args.length == 0 ? "localhost" : args[0];
        StringClientSocket clientSocket = new StringClientSocket(new Socket(address, 9382));

        /** send initial command */
        Tensor config = Tensors.fromString("{SanFrancisco}");
        config.append(RealScalar.of(0.4));
        config.append(RealScalar.of(177));
        clientSocket.writeln(config);

        /** receive initial information */
        Tensor initialInfo = Tensors.fromString(clientSocket.readLine());
        Tensor minX = initialInfo.get(0);
        Tensor minY = initialInfo.get(1);

        /** receive dispatching status and send dispatching command */
        BasicDispatchingTestLogic bdl = new BasicDispatchingTestLogic();

        while (true) {
            Tensor status = Tensors.fromString(clientSocket.readLine());
            if (Tensors.isEmpty(status))
                break;
            Tensor command = bdl.of(status);
            clientSocket.writeln(command);
        }

        Tensor finalInfo = Tensors.fromString(clientSocket.readLine());
        System.out.println("finalInfo: " + finalInfo);

    }

}
