/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import amod.aido.AidoHost;
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
        // Previously:
        // if i put 2 instead of 1 -> doesn't finish but exception in traffic link data
        // ... cannot run more than 1 scenario at runtime
        // TODO Jan right now SanFrancisco has to run last -> don't shuffle
        // Collections.shuffle(list);
        list = list.stream().limit(4).collect(Collectors.toList());
        for (int index : list)
            switch (index) {
            case 0:
                guest().run("TelAviv", 0.001, 6);
                break;
            case 1:
                guest().run("Santiago", 0.001, 10);
                break;
            case 2:
                guest().run("Berlin", 0.002, 10);
                break;
            case 3:
                guest().run("SanFrancisco", 0.01, 5);
                break;
            default:
                throw new RuntimeException("out of range");
            }

    }

}
