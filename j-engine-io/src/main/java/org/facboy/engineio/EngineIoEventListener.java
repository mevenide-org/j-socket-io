package org.facboy.engineio;

import org.facboy.engineio.protocol.Transport;

/**
 * @author Christopher Ng
 */
public interface EngineIoEventListener {
    void onConnection(ConnectionEvent event);

    void onMessage(MessageEvent event);

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

    class MessageEvent {
        private final String sessionId;
        private final Object message;

        public MessageEvent(String sessionId, Object message) {
            this.sessionId = sessionId;
            this.message = message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public Object getMessage() {
            return message;
        }
    }

}
