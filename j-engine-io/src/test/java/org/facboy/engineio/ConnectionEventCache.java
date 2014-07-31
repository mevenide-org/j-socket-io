package org.facboy.engineio;

import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.facboy.engineio.EngineIoEventListener.ConnectionEvent;
import org.facboy.engineio.jersey.EventWrapper;

/**
 * @author Christopher Ng
 */
@Singleton
public class ConnectionEventCache {
    private final NavigableMap<UUID, EventWrapper<ConnectionEvent>> connectionEvents = new ConcurrentSkipListMap<UUID, EventWrapper<ConnectionEvent>>();

    @Inject
    public ConnectionEventCache(EngineIo engineIo) {
        engineIo.addEventListener(new DefaultEngineIoEventListener() {
            @Override
            public void onConnection(ConnectionEvent event) {
                EventWrapper<ConnectionEvent> wrapper = new EventWrapper<ConnectionEvent>(event);
                connectionEvents.put(wrapper.getUuid(), wrapper);
            }
        });
    }

    public NavigableMap<UUID, EventWrapper<ConnectionEvent>> getConnectionEvents() {
        return connectionEvents;
    }
}
