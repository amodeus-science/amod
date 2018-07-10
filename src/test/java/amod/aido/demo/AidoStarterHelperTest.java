/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.IOException;
import java.net.UnknownHostException;

import amod.aido.AidoHost;
import junit.framework.TestCase;

public class AidoStarterHelperTest extends TestCase {
    public void testSimple() throws UnknownHostException, IOException, Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AidoHost.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(1000);

        AidoGuest aidoGuest = new AidoGuest("localhost");
        aidoGuest.run("SanFrancisco", 0.01, 5);

    }
}
