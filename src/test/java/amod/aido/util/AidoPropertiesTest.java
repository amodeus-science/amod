/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido.util;

import java.util.Properties;

import amod.aido.core.ScoreParameters;
import ch.ethz.idsc.tensor.io.ResourceData;
import junit.framework.TestCase;

public class AidoPropertiesTest extends TestCase {
    public void testScoreParam() {
        assertNotNull(ScoreParameters.GLOBAL);
    }

    public void testScenarios() {
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        assertNotNull(properties);
    }
}
