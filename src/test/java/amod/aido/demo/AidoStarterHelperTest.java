/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import amod.aido.AidoHost;
import ch.ethz.idsc.amodeus.aido.CleanAidoScenarios;
import ch.ethz.idsc.amodeus.util.io.FileDelete;
import ch.ethz.idsc.tensor.io.ResourceData;
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

        Properties properties = ResourceData.properties("/aido/scenarios.properties");

        List<String> list = new ArrayList<>(properties.stringPropertyNames());

        Collections.shuffle(list);

        list = list.subList(0, 3);

        Random random = new Random();
        for (String index : list) {

            guest().run(index, 800 + random.nextInt(200), 6 + random.nextInt(10));

            /** files */
            CleanAidoScenarios.now();
            /** virtual network folder */
            FileDelete.of(new File("virtualNetwork"), 1, 4);
            /** output folder */
            FileDelete.of(new File("output"), 5, 25000);
        }
    }
}
