package amod.aido.util;

import java.util.Properties;

import ch.ethz.idsc.tensor.io.ResourceData;
import junit.framework.TestCase;

public class AidoPropertiesTest extends TestCase {
    public void testScoreParam() {
        Properties properties = ResourceData.properties("/aido/scoreparam.properties");
        assertNotNull(properties);
    }

    public void testScenarios() {
        Properties properties = ResourceData.properties("/aido/scenarios.properties");
        assertNotNull(properties);
    }
}
