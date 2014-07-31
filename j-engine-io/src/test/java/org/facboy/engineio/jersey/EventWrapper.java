package org.facboy.engineio.jersey;

import java.util.UUID;

import com.eaio.uuid.UUIDGen;

/**
 * @author Christopher Ng
 */
public class EventWrapper<T> {
    private final UUID uuid;
    private T event;

    public EventWrapper(T event) {
        this.uuid = new UUID(UUIDGen.newTime(), UUIDGen.getClockSeqAndNode());
        this.event = event;
    }



    public UUID getUuid() {
        return uuid;
    }

    public T getEvent() {
        return event;
    }
}
