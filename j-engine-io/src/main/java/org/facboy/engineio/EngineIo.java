package org.facboy.engineio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIoEventListener.ConnectionEvent;
import org.facboy.engineio.id.IdGenerator;
import org.facboy.engineio.id.SessionRegistry;
import org.facboy.engineio.protocol.ProtocolError;
import org.facboy.engineio.protocol.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
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
    private final Set<EngineIoEventListener> eventListeners = Sets.newCopyOnWriteArraySet();

    private int pingInterval = DEFAULT_PING_INTERVAL;
    private int pingTimeout = DEFAULT_PING_TIMEOUT;
    private Set<String> transports = DEFAULT_TRANSPORTS;

    @Inject
    public EngineIo(IdGenerator idGenerator, SessionRegistry sessionRegistry) {
        this.idGenerator = idGenerator;
        this.sessionRegistry = sessionRegistry;
    }

    public void checkRequest(String protocol, String transport, String sid) throws EngineIoException {
        checkProtocol(protocol);
        checkTransport(transport);

        if (sid != null) {
            Boolean hasSession = sessionRegistry.getSession(sid);
            if (hasSession == null) {
                throw new EngineIoException(ProtocolError.UNKNOWN_SID, HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    private void checkProtocol(String protocol) throws EngineIoException {
        if (!Objects.equal(protocol, "2") && !Objects.equal(protocol, "3")) {
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

    public void onRequest(String transport, String sid, Response resp) {
        if (Strings.isNullOrEmpty(sid)) {
            sendHandshake(resp, transport);
        } else {
            Boolean hasSession = sessionRegistry.getSession(sid);
            if (hasSession == null) {
                throw new RuntimeException("Unknown sid received: " + sid);
            }
            resp.startAsync();
        }
    }

    private void sendHandshake(Response resp, String transport) {
        Transport transportObj = Transport.valueOf(transport);

        HandshakeResponse handshakeResponse = new HandshakeResponse();
        handshakeResponse.sid = idGenerator.generateId();
        handshakeResponse.upgrades = ImmutableSet.copyOf(Sets.intersection(transports, transportObj.getUpgrades()));
        handshakeResponse.pingInterval = pingInterval;
        handshakeResponse.pingTimeout = pingTimeout;

        sessionRegistry.registerSession(handshakeResponse.sid);

        resp.sendHandshake(handshakeResponse);

        for (EngineIoEventListener eventListener : eventListeners) {
            try {
                eventListener.onConnection(new ConnectionEvent(handshakeResponse.sid, transportObj));
            } catch (Exception e) {
                logger.error("Error handling connection event:", e);
            }
        }
    }

    public interface Response {
        void sendHandshake(HandshakeResponse handshakeResponse);

        /**
         * TODO will need an actual object
         */
        void startAsync();
    }

    public void addEventListener(@Nonnull EngineIoEventListener eventListener) {
        checkNotNull(eventListener, "eventListener is null");
        eventListeners.add(eventListener);
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
