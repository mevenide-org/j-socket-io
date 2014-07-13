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
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.StringPacket;

/**
 * @author Christopher Ng
 */
public class EngineIoServlet extends HttpServlet {
    public static final String PROTOCOL = "EIO";
    public static final String TRANSPORT = "transport";

    public enum Code {
        TRANSPORT_UNKNOWN(0),
        BAD_REQUEST(3);

        private final int code;

        private Code(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    private final PayloadWriter payloadWriter = new PayloadWriter();

    private IdGenerator idGenerator;
    private ObjectMapper objectMapper;
    private int pingInterval = 25000;
    private int pingTimeout = 60000;

    @Override
    public void init() throws ServletException {
        idGenerator = new Base64IdGenerator();
        objectMapper = ObjectMapperFactory.getInstance().getObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            checkProtocol(req);

            String transport = req.getParameter(TRANSPORT);
            if (Objects.equal(transport, "websocket")) {
                throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Code.BAD_REQUEST.code(), "Bad request");
            }
            if (!Objects.equal(transport, "polling")) {
                throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST, Code.TRANSPORT_UNKNOWN.code(), "Transport unknown");
            }

            HandshakeResponse handshakeResponse = new HandshakeResponse();
            handshakeResponse.sid = idGenerator.generateId();
            handshakeResponse.upgrades = ImmutableList.of();
            handshakeResponse.pingInterval = pingInterval;
            handshakeResponse.pingTimeout = pingTimeout;

            // TODO not always going to be binary
            resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
            // TODO make this optional
            resp.addCookie(new Cookie("io", handshakeResponse.sid));

            payloadWriter.writePayload(resp.getOutputStream(), new StringPacket(Type.OPEN, objectMapper.writeValueAsString(handshakeResponse)));
        } catch (EngineIoException e) {
            handleError(e, resp);
        }
    }

    private void checkProtocol(HttpServletRequest req) throws EngineIoException {
        String protocol = req.getParameter(PROTOCOL);
        if (!Objects.equal(protocol, "2") && !Objects.equal(protocol, "3")) {
            // TODO is BAD_REQUEST the right code?
            throw new EngineIoException(HttpServletResponse.SC_BAD_REQUEST,
                    Code.TRANSPORT_UNKNOWN.code(), "Unknown protocol: " + protocol);
        }
    }

    private void handleError(EngineIoException engineIoException, HttpServletResponse response) throws IOException {
        response.setStatus(engineIoException.getStatusCode());
        objectMapper.writeValue(response.getOutputStream(), engineIoException);
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
