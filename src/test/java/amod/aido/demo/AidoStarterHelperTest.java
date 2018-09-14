/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import amod.aido.AidoHost;
import ch.ethz.idsc.amodeus.aido.CleanAidoScenarios;
import ch.ethz.idsc.amodeus.util.io.FileDelete;
import junit.framework.TestCase;

public class AidoStarterHelperTest extends TestCase {
    private static AidoGuest guest() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AidoHost.main(null);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(1000);

        return new AidoGuest("localhost");

    }

    public void testSanFrancisco() throws UnknownHostException, IOException, Exception {

        List<Integer> list = IntStream.range(0, 4).boxed().collect(Collectors.toList());
        Collections.shuffle(list);
        for (int index : list)
            switch (index) {
            case 0:
                guest().run("TelAviv", 1000, 6);
                break;
            case 1:
                guest().run("Santiago", 1000, 10);
                break;
            case 2:
                guest().run("Berlin", 1000, 10);
                break;
            case 3:
                guest().run(AidoGuest.SCENARIO, 1000, 5);
                break;
            default:
                throw new RuntimeException("out of range");
            }

        /** files */
        CleanAidoScenarios.now();
        /** virtual network folder */
        FileDelete.of(new File("virtualNetwork"), 1, 4);
        /** output folder */
        FileDelete.of(new File("output"), 5, 25000);
    }
}
