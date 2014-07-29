package org.facboy.engineio;

import org.facboy.engineio.EngineIo.Transport;

/**
 * @author Christopher Ng
 */
public interface EngineIoEventListener {
    void onConnection(ConnectionEvent event);

    class ConnectionEvent {
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
}
