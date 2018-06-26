/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;

/** @author onicolo 06-2018
 *         Created temporarily for running AmodScenarioVideoMaker -> to load v2 network. Copy from
 *         NetworkLoader in Amodidsc */
/* package */ enum StaticHelper {
    ;

    public static Network loadNetwork(File networkFile) {
        Network network = NetworkUtils.createNetwork();
        new NetworkReaderMatsimV2(network).readFile(networkFile.getAbsolutePath());
        return network;
    }
}
