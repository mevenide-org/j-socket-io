package org.facboy.engineio.event;

import org.facboy.engineio.protocol.Packet;
import org.facboy.engineio.session.Session;

/**
* @author Christopher Ng
*/
public class MessageEvent {
    private final Session session;
    private final Packet message;

    public MessageEvent(Session session, Packet message) {
        this.session = session;
        this.message = message;
    }

    public Session getSession() {
        return session;
    }

    public Packet getMessage() {
        return message;
    }
}
