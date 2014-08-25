package org.facboy.engineio.protocol;

import java.util.Set;

import org.facboy.engineio.session.Session;

import com.google.common.collect.ImmutableSet;

/**
* @author Christopher Ng
*/
public enum Transport {
    polling(ImmutableSet.of("websocket")),
    websocket(ImmutableSet.<String>of());

    private final Set<String> upgrades;
    /**
     * Dummy session used when handshaking.  A real session will be generated for the actual handshake.
     */
    @SuppressWarnings("ConstantConditions")
    private final Session handshakeSession = new Session(null, this);

    private Transport(Set<String> upgrades) {
        this.upgrades = upgrades;
    }

    public Set<String> getUpgrades() {
        return upgrades;
    }

    public Session getHandshakeSession() {
        return handshakeSession;
    }
}
