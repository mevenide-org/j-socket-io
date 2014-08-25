package org.facboy.engineio.event;

import org.facboy.engineio.protocol.Transport;

/**
* @author Christopher Ng
*/
public class ConnectionEvent {
    private final String sessionId;
    private final Transport transport;

    public ConnectionEvent(String sessionId, Transport transport) {
        this.sessionId = sessionId;
        this.transport = transport;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Transport getTransport() {
        return transport;
    }
}
