/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.aido;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;

import ch.ethz.idsc.aido.core.CleanAidoScenarios;
import ch.ethz.idsc.aido.demo.AidoGuest;
import ch.ethz.idsc.tensor.io.DeleteDirectory;
import ch.ethz.idsc.tensor.io.ResourceData;
import junit.framework.TestCase;

public class AidoHostTest extends TestCase {
    private static Exception hostException = null;

    private static AidoGuest guest() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AidoHost.main(null);
                } catch (Exception exception) {
                    // exception.printStackTrace();
                    hostException = exception;
                }
            }
        }).start();
        Thread.sleep(1000);
        return new AidoGuest("localhost");
    }

    public void testScenario() throws UnknownHostException, IOException, Exception {
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        List<String> list = new ArrayList<>(properties.stringPropertyNames());
        Collections.shuffle(list);
        list = list.subList(0, 1);
        Random random = new Random();
        for (String index : list) {
            try {
                guest().run(index, 800 + random.nextInt(200), 6 + random.nextInt(10));
            } finally {
                /** files */
                CleanAidoScenarios.now();
                { // folder should not exist
                    /** virtual network file "virtualNetwork" should not exist for AIDO */
                    File file = new File("virtualNetwork");
                    if (file.exists())
                        DeleteDirectory.of(file, 1, 4);
                }
                {
                    /** output folder */
                    File file = new File("output");
                    if (file.isDirectory())
                        DeleteDirectory.of(file, 5, 25000);
                }

                if (Objects.nonNull(hostException))
                    throw hostException;
            }
        }
    }
}
