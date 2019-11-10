/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.aido.core;

import java.util.Properties;

import ch.ethz.idsc.tensor.io.ResourceData;
import junit.framework.TestCase;

public class ScoreParametersTest extends TestCase {
    public void testScoreParam() {
        assertNotNull(ScoreParameters.GLOBAL);
    }

    public void testScenarios() {
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        assertNotNull(properties);
    }
}
