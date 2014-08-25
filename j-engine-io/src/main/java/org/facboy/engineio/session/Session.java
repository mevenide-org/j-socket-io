package org.facboy.engineio.session;

import javax.annotation.Nonnull;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;
import org.facboy.engineio.protocol.Transport;

/**
 * @author Christopher Ng
 */
public class Session {
    private final String id;
    private final Transport transport;

    /**
     * TODO shite
     */
    private volatile Sender sender;

    /**
     *
     * @param id is only non-null for internal sentinel sessions.
     * @param transport
     */
    public Session(String id, @Nonnull Transport transport) {
        this.id = id;
        this.transport = transport;
    }

    public String getId() {
        return id;
    }

    public Transport getTransport() {
        return transport;
    }

    public boolean isHandshake() {
        return id == null;
    }

    public void send(StringPacket packet) {
        if (sender != null) {
            sender.send(packet);
        }
    }

    public void send(BinaryPacket packet) {
        if (sender != null) {
            sender.send(packet);
        }
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public interface Sender {
        void send(StringPacket packet);

        void send(BinaryPacket packet);
    }
}
