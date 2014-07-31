package org.facboy.engineio.protocol;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
* @author Christopher Ng
*/
public enum Transport {
    polling(ImmutableSet.of("websocket")),
    websocket(ImmutableSet.<String>of());

    private final Set<String> upgrades;

    private Transport(Set<String> upgrades) {
        this.upgrades = upgrades;
    }

    public Set<String> getUpgrades() {
        return upgrades;
    }
}
