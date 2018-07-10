/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.ext;

import junit.framework.TestCase;

public class UserLocationSpecsTest extends TestCase {
    public void testSimple() {
        assertNotNull(UserLocationSpecs.SANFRANCISCO.center());
    }
}
