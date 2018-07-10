/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.Objects;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.idsc.amodeus.util.net.StringSocket;

public class AidoModule extends AbstractModule {
    private final StringSocket stringSocket;

    public AidoModule(StringSocket stringSocket) {
        this.stringSocket = Objects.requireNonNull(stringSocket);
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
}
