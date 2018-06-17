/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.net.MalformedURLException;

/** only one ScenarioServer can run at one time, since a fixed network port is
 * reserved to serve the simulation status */
public enum ScenarioStandalone {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        ScenarioPreparer.main(args);
        ScenarioServer.main(args);
    }

}