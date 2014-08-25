package org.facboy.engineio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.event.ConnectionEvent;
import org.facboy.engineio.event.EngineIoEventListener;
import org.facboy.engineio.event.MessageEvent;
import org.facboy.engineio.protocol.Packet;
import org.facboy.engineio.protocol.ProtocolError;
import org.facboy.engineio.protocol.Transport;
import org.facboy.engineio.session.IdGenerator;
import org.facboy.engineio.session.Session;
import org.facboy.engineio.session.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author Christopher Ng
 */
@Singleton
public class EngineIo {
    private static final Logger logger = LoggerFactory.getLogger(EngineIo.class);

    public static final int DEFAULT_PING_INTERVAL = 25000;
    public static final int DEFAULT_PING_TIMEOUT = 60000;

    private static final Set<String> DEFAULT_TRANSPORTS = ImmutableSet.copyOf(Collections2.transform(
            EnumSet.allOf(Transport.class), Functions.toStringFunction()));

    private final IdGenerator idGenerator;
    private final SessionRegistry sessionRegistry;

    private final Set<EngineIoEventListener<ConnectionEvent>> connectionEventListeners = Sets.newCopyOnWriteArraySet();
    private final Set<EngineIoEventListener<MessageEvent>> messageEventListeners = Sets.newCopyOnWriteArraySet();

    private int pingInterval = DEFAULT_PING_INTERVAL;
    private int pingTimeout = DEFAULT_PING_TIMEOUT;
    private Set<String> transports = DEFAULT_TRANSPORTS;
    private boolean allowUpgrades = true;

    @Inject
    public EngineIo(IdGenerator idGenerator, SessionRegistry sessionRegistry) {
        this.idGenerator = idGenerator;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Gets an existing session, or returns a handshake session if sid is null.
     * @param protocol
     * @param transport
     * @param sid
     * @return Session if it exists
     * @throws EngineIoException
     */
    public Session getSession(String protocol, String transport, String sid) throws EngineIoException {
        Session session;

        checkProtocol(protocol);
        if (sid == null) {
            checkTransport(transport);
            Transport transportObj = Transport.valueOf(transport);
            session = transportObj.getHandshakeSession();
        } else {
            session = sessionRegistry.getSession(sid);
            if (session == null) {
                throw new EngineIoException(ProtocolError.UNKNOWN_SID, HttpServletResponse.SC_BAD_REQUEST);
            }
            if (!transport.equals(session.getTransport().name())) {
                throw new EngineIoException(ProtocolError.BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        return session;
    }

    private void checkProtocol(String protocol) throws EngineIoException {
        if (protocol != null && !Objects.equal(protocol, "2") && !Objects.equal(protocol, "3")) {
            logger.warn("Unknown protocol: {}", protocol);
        }
    }

    private void checkTransport(String transport) throws EngineIoException {
        if (!transports.contains(transport)) {
            throw new EngineIoException(ProtocolError.UNKNOWN_TRANSPORT, HttpServletResponse.SC_BAD_REQUEST);
        }
        try {
            Transport.valueOf(transport);
        } catch (IllegalArgumentException e) {
            throw new EngineIoException(ProtocolError.UNKNOWN_TRANSPORT, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public void handshake(Session session, HandshakeSender resp) {
        assert session.isHandshake();
        // generate the real session
        session = new Session(idGenerator.generateId(), session.getTransport());

        HandshakeResponse handshakeResponse = new HandshakeResponse();
        handshakeResponse.sid = session.getId();
        handshakeResponse.upgrades = allowUpgrades ? ImmutableSet.copyOf(Sets.intersection(transports,
                session.getTransport().getUpgrades())) : ImmutableSet.<String>of();
        handshakeResponse.pingInterval = pingInterval;
        handshakeResponse.pingTimeout = pingTimeout;

        resp.sendHandshake(handshakeResponse);

        sessionRegistry.registerSession(session);

        for (EngineIoEventListener<ConnectionEvent> eventListener : connectionEventListeners) {
            try {
                eventListener.onEvent(new ConnectionEvent(handshakeResponse.sid, session.getTransport()));
            } catch (Exception e) {
                logger.error("Error handling connection event:", e);
            }
        }
    }

    /**
     * TODO does this belong here?
     * @param fromSession
     * @param packet
     */
    public void onMessage(Session fromSession, Packet packet) {
        MessageEvent messageEvent = new MessageEvent(fromSession, packet);
        for (EngineIoEventListener<MessageEvent> eventListener : messageEventListeners) {
            eventListener.onEvent(messageEvent);
        }
    }

    public interface HandshakeSender {
        void sendHandshake(HandshakeResponse handshakeResponse);
    }

    public void addConnectionEventListener(@Nonnull EngineIoEventListener<ConnectionEvent> eventListener) {
        checkNotNull(eventListener, "eventListener is null");
        connectionEventListeners.add(eventListener);
    }

    public void addMessageEventListener(@Nonnull EngineIoEventListener<MessageEvent> eventListener) {
        checkNotNull(eventListener, "eventListener is null");
        messageEventListeners.add(eventListener);
    }

    public int getPingInterval() {
        return pingInterval;
    }

    @Inject(optional = true)
    public void setPingInterval(@Named("engine.io.pingInterval") int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    @Inject(optional = true)
    public void setPingTimeout(@Named("engine.io.pingTimeout") int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public boolean isAllowUpgrades() {
        return allowUpgrades;
    }

    @Inject(optional = true)
    public void setAllowUpgrades(@Named("engine.io.allowUpgrades") boolean allowUpgrades) {
        // don't try to be clever and alter transports to empty here, transports is used to validate
        // request transports too, not just for upgrades
        this.allowUpgrades = allowUpgrades;
    }

    public Set<String> getTransports() {
        return transports;
    }

    @Inject(optional = true)
    public void setTransports(@Named("engine.io.transports") Collection<String> transports) {
        this.transports = ImmutableSet.copyOf(transports);
    }

    public static class HandshakeResponse {
        public String sid;
        public Collection<String> upgrades;
        public int pingInterval;
        public int pingTimeout;
    }
}
