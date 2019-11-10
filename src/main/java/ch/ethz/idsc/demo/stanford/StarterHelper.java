/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.demo.stanford;

import ch.ethz.idsc.aido.AidoHost;
import ch.ethz.idsc.aido.demo.AidoGuest;

/** function starts {@link AidoHost} process and {@link AidoGuest} process
 * in different threads for testing. */
/* package */ enum StarterHelper {
    ;

    public static void main(String[] args) throws Exception {
        /** {@link AidoHost} runs a simulation of an autonomous mobility-on-demand
         * system in the AMoDeus framework */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringHost.main(args);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
        /* Thread.sleep(1000); */
        /** {@link AidoGuest} executes the dispatching logic of the user participating
         * in the artificial intelligence driving olympics (AIDO) */
        /* AidoGuest.main(args); */
    }
}
