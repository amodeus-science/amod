/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.demo;

import amod.aido.AidoHost;

public enum AidoStarterHelper {
    ;

    public static void main(String[] args) throws Exception {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AidoHost.main(args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(1000);

        AidoGuest.main(args);

    }

}
