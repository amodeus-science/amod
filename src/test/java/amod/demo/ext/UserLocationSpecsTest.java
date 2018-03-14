package amod.demo.ext;

import junit.framework.TestCase;

public class UserLocationSpecsTest extends TestCase {
    public void testSimple() {
        assertNotNull(UserLocationSpecs.SANFRANCISCO.center());
    }
}
