/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import ch.ethz.idsc.amodeus.aido.*;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.*;

import java.io.File;
import java.util.*;

/**
 * Usage: java -cp target/amod-VERSION.jar amod.aido.AidoHost [city]
 */

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
            String scenarioName = getScenarioName(args);
            String scenarioPath = MultiFileTools.getWorkingDirectory().getAbsolutePath() + "/" + scenarioName + "/";
            System.out.println("Using scenario directory: " + scenarioPath);
            File workingDirectory = new File(scenarioPath);
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

    private static String getScenarioName(String[] args) {
        String cityDir = args.length == 0 ? "SanFrancisco" : args[0];
        List<String> validCities = Arrays.asList("SanFrancisco", "BerlinAMoDeus", "SantiagoAMoDeus", "TelavivAMoDeus");
        String usage = "AidoHost takes a single city in :" + validCities.toString();
        if (!validCities.contains(cityDir))
            throw new IllegalArgumentException(usage);

        return cityDir;
    }
}
