package org.facboy.engineio;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.facboy.engineio.id.Base64IdGenerator;
import org.facboy.engineio.id.IdGenerator;
import org.facboy.engineio.id.IdRegistry;
import org.facboy.engineio.payload.Base64PayloadWriter;
import org.facboy.engineio.payload.PayloadWriter;
import org.facboy.engineio.payload.Xhr2PayloadWriter;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christopher Ng
 */
public class EngineIoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EngineIoServlet.class);

    public static final String PROTOCOL = "EIO";
    public static final String TRANSPORT = "transport";
    public static final String SESSION_ID = "sid";
    public static final String BASE64 = "b64";

    public enum Code {
        TRANSPORT_UNKNOWN(0),
        SESSION_ID_UNKNOWN(1),
        BAD_REQUEST(3);

        private final int code;

        private Code(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    private final PayloadWriter xhr2PayloadWriter = new Xhr2PayloadWriter();
    private final PayloadWriter base64PayloadWriter = new Base64PayloadWriter();

    private IdGenerator idGenerator;
    private ObjectMapper objectMapper;
    private IdRegistry sessionIdRegistry;
    private int pingInterval = 25000;
    private int pingTimeout = 60000;

    @Override
    public void init() throws ServletException {
        idGenerator = new Base64IdGenerator();
        sessionIdRegistry = new IdRegistry();
        objectMapper = ObjectMapperFactory.getInstance().getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        new GetHandler(req, resp).handle();
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
                if (Objects.equal(transport, "websocket")) {
                    throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Code.BAD_REQUEST.code(), "Bad request");
                }
                if (!Objects.equal(transport, "polling")) {
                    throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST,
                            Code.TRANSPORT_UNKNOWN.code(),
                            "Transport unknown");
                }

                String sid = req.getParameter(SESSION_ID);
                if (!Strings.isNullOrEmpty(sid)) {
                    String beh = sessionIdRegistry.getSid(sid);
                    if (beh == null) {
                        throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Code.SESSION_ID_UNKNOWN.code(), "Session ID unknown");
                    }
                }

                HandshakeResponse handshakeResponse = new HandshakeResponse();
                handshakeResponse.sid = idGenerator.generateId();
                handshakeResponse.upgrades = ImmutableList.of();
                handshakeResponse.pingInterval = pingInterval;
                handshakeResponse.pingTimeout = pingTimeout;

                // TODO make this optional
                resp.addCookie(new Cookie("io", handshakeResponse.sid));

                payloadWriter.writePayload(resp,
                        new StringPacket(Type.OPEN, objectMapper.writeValueAsString(handshakeResponse)));
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

        private void handleError(EngineIoException engineIoException) throws IOException {
            resp.setStatus(engineIoException.getStatusCode());
            resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
            objectMapper.writeValue(resp.getOutputStream(), engineIoException);
            resp.getOutputStream().flush();
        }
    }


    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public static class HandshakeResponse {
        public String sid;
        public List<String> upgrades;
        public int pingInterval;
        public int pingTimeout;
    }
}
