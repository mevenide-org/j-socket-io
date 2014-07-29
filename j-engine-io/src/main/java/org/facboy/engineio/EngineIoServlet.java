package org.facboy.engineio;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIoEventListener.ConnectionEvent;
import org.facboy.engineio.payload.PayloadWriter;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Christopher Ng
 */
@Singleton
public class EngineIoServlet extends HttpServlet {
    private final EngineIo engineIo;

    @Inject
    public EngineIoServlet(EngineIo engineIo) {
        this.engineIo = engineIo;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        engineIo.handleGet(req, resp);
    }

    class GetHandler {
        private final HttpServletRequest req;
        private final HttpServletResponse resp;
        private final PayloadWriter payloadWriter;

        GetHandler(HttpServletRequest req, HttpServletResponse resp) {
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

                setOriginHeaders();

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

        private void setOriginHeaders() {
            String originHeader = req.getHeader(HttpHeaders.ORIGIN);
            if (Strings.isNullOrEmpty(originHeader)) {
                resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            } else {
                resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);
                resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
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
}
