/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amod;

import java.net.MalformedURLException;

/** Helper class to run a default preparer and server, the typical
 * sequence of execution. */
/* package */ enum ScenarioExecutionSequence {
    ;

    public static void main(String[] args) throws MalformedURLException, Exception {
        // preliminary steps, e.g., seting up data structures required by operational policies
        ScenarioPreparer.main(args);
        // running the simulation
        ScenarioServer.main(args);
        // viewing results, the viewer can also connect to a running simulation.
        ScenarioViewer.main(args);
    }
}
