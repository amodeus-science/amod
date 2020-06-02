/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket.demo;

import amodeus.socket.SocketHost;

/** function starts {@link SocketHost} process and {@link SocketGuest} process
 * in different threads for testing. */
/* package */ enum SocketStarterHelper {
    ;

    public static void main(String[] args) throws Exception {
        /** {@link SocketHost} runs a simulation of an autonomous mobility-on-demand
         * system in the AMoDeus framework */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketHost.main(args);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(1000);

        /** {@link SocketGuest} executes the dispatching logic encoded
         * in another programming language */
        SocketGuest.main(args);
    }
}
