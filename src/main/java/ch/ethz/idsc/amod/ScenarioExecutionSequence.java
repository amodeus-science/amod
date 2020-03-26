/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod;

import java.net.MalformedURLException;

/** Helper class to run a default preparer and server, the typical 
 * sequence of execution. */
public enum ScenarioExecutionSequence {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        ScenarioPreparer.main(args);
        ScenarioServer.main(args);
    }

}