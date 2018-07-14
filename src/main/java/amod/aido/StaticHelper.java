/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

/* package */ enum StaticHelper {
    ;
    static void setup() {
        System.setProperty("matsim.preferLocalDtds", "true");
    }
}
