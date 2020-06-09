/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket;

import java.util.Objects;

import amodeus.amodeus.util.net.StringSocket;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SocketModule extends AbstractModule {
    private final StringSocket stringSocket;
    private final int numReqTot;

    public SocketModule(StringSocket stringSocket, int numReqTot) {
        this.stringSocket = Objects.requireNonNull(stringSocket);
        this.numReqTot = numReqTot;
    }

    @Override
    public void install() {
        // ---
    }

    @Provides
    @Singleton
    public StringSocket provideStringSocket(Network network) {
        return stringSocket;
    }

    @Provides
    @Singleton
    public int provideNumReqTot() {
        return numReqTot;
    }
}
