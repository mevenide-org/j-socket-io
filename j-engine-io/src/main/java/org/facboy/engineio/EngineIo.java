package org.facboy.engineio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIoEventListener.ConnectionEvent;
import org.facboy.engineio.id.IdGenerator;
import org.facboy.engineio.id.SessionRegistry;
import org.facboy.engineio.payload.Base64PayloadWriter;
import org.facboy.engineio.payload.PayloadWriter;
import org.facboy.engineio.payload.Xhr2PayloadWriter;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author Christopher Ng
 */
@Singleton
public class EngineIo {
    private static final Logger logger = LoggerFactory.getLogger(EngineIo.class);

    public static final String PROTOCOL = "EIO";
    public static final String TRANSPORT = "transport";
    public static final String SESSION_ID = "sid";
    public static final String BASE64 = "b64";

    public static final String DEFAULT_COOKIE = "io";
    public static final int DEFAULT_PING_INTERVAL = 25000;
    public static final int DEFAULT_PING_TIMEOUT = 60000;

    private static final Set<String> DEFAULT_TRANSPORTS = ImmutableSet.copyOf(Collections2.transform(
            EnumSet.allOf(Transport.class), new Function<Transport, String>() {
                @Nullable
                @Override
                public String apply(@Nullable Transport input) {
                    return input.name().toLowerCase();
                }
            }));

    public enum Error {
        UNKNOWN_TRANSPORT("Transport unknown"),
        UNKNOWN_SID("Session ID unknown"),
        BAD_HANDSHAKE_METHOD("Bad handshake method"),
        BAD_REQUEST("Bad request");

        private final String message;

        Error(String message) {
            this.message = message;
        }

        public int code() {
            return ordinal();
        }

        public String message() {
            return message;
        }
    }

    public enum Transport {
        POLLING,
        WEBSOCKET
    }

    private final PayloadWriter xhr2PayloadWriter = new Xhr2PayloadWriter();
    private final PayloadWriter base64PayloadWriter = new Base64PayloadWriter();

    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final SessionRegistry sessionRegistry;
    private final Set<EngineIoEventListener> eventListeners = Sets.newCopyOnWriteArraySet();

    private int pingInterval = DEFAULT_PING_INTERVAL;
    private int pingTimeout = DEFAULT_PING_TIMEOUT;
    private Set<String> transports = DEFAULT_TRANSPORTS;

    private String cookie = DEFAULT_COOKIE;

    @Inject
    public EngineIo(IdGenerator idGenerator, ObjectMapper objectMapper,
            SessionRegistry sessionRegistry) {
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
    }

    public void handleGet(EngineIoRequest req, EngineIoResponse resp) throws ServletException, IOException {
        new GetHandler(req, resp).handle();
    }

    class GetHandler {
        private final EngineIoRequest req;
        private final EngineIoResponse resp;
        private final PayloadWriter payloadWriter;

        GetHandler(EngineIoRequest req, EngineIoResponse resp) {
            this.req = req;
            this.resp = resp;
            if (Objects.equal(req.getParameter(BASE64), "1")) {
                payloadWriter = base64PayloadWriter;
            } else {
                payloadWriter = xhr2PayloadWriter;
            }
        }

        public void handle() throws IOException {
            try {
                checkProtocol();

                String transport = req.getParameter(TRANSPORT);
                if (!transports.contains(transport)) {
                    throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Error.UNKNOWN_TRANSPORT);
                }

                String sid = req.getParameter(SESSION_ID);
                if (Strings.isNullOrEmpty(sid)) {
                    sendHandshake(transport);
                } else {
                    Boolean hasSession = sessionRegistry.getSession(sid);
                    if (hasSession == null) {
                        throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Error.UNKNOWN_SID);
                    }
                    req.startAsync();
                }
            } catch (EngineIoException e) {
                handleError(e);
            }
        }

        private void checkProtocol() throws EngineIoException {
            String protocol = req.getParameter(PROTOCOL);
            if (!Objects.equal(protocol, "2") && !Objects.equal(protocol, "3")) {
                logger.warn("Unknown protocol: {}", protocol);
            }
        }

        private void sendHandshake(String transport) throws IOException {
            HandshakeResponse handshakeResponse = new HandshakeResponse();
            handshakeResponse.sid = idGenerator.generateId();
            handshakeResponse.upgrades = transports;
            handshakeResponse.pingInterval = pingInterval;
            handshakeResponse.pingTimeout = pingTimeout;

            // register session
            sessionRegistry.registerSession(handshakeResponse.sid);

            if (cookie != null) {
                resp.addCookie(new Cookie(cookie, handshakeResponse.sid));
            }

            payloadWriter.writePayload(resp,
                    new StringPacket(Type.OPEN, objectMapper.writeValueAsString(handshakeResponse)));

            for (EngineIoEventListener eventListener : eventListeners) {
                try {
                    eventListener.onConnection(new ConnectionEvent(handshakeResponse.sid, Transport.valueOf(transport.toUpperCase())));
                } catch (Exception e) {
                    logger.error("Error handling connection event:", e);
                }
            }
        }

        private void handleError(EngineIoException engineIoException) throws IOException {
            resp.setStatus(engineIoException.getStatusCode());
            resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
            objectMapper.writeValue(resp.getOutputStream(), engineIoException);
            resp.getOutputStream().flush();
        }
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

    @Inject(optional = true)
    public void setCookie(@Named("engine.io.cookie") String cookie) {
        this.cookie = cookie.trim();
    }

    @Inject(optional = true)
    public void setCookieEnabled(@Named("engine.io.cookieEnabled") boolean cookieEnabled) {
        // this is deliberate, we are using this as a sentinel value
        if (cookieEnabled) {
            this.cookie = DEFAULT_COOKIE;
        } else {
            this.cookie = null;
        }
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
