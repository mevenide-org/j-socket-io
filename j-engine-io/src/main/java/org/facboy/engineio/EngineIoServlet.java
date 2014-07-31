package org.facboy.engineio;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIo.Response;
import org.facboy.engineio.payload.Base64PayloadWriter;
import org.facboy.engineio.payload.PayloadWriter;
import org.facboy.engineio.payload.Xhr2PayloadWriter;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.Parameter;
import org.facboy.engineio.protocol.StringPacket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * This implements the polling transport.
 *
 * @author Christopher Ng
 */
@Singleton
public class EngineIoServlet extends HttpServlet {
    public static final String DEFAULT_COOKIE = "io";

    private final PayloadWriter xhr2PayloadWriter = new Xhr2PayloadWriter();
    private final PayloadWriter base64PayloadWriter = new Base64PayloadWriter();

    private final EngineIo engineIo;
    private final ObjectMapper objectMapper;

    private String cookie = DEFAULT_COOKIE;

    @Inject
    public EngineIoServlet(EngineIo engineIo, ObjectMapper objectMapper) {
        this.engineIo = engineIo;
        this.objectMapper = objectMapper;
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
            if (Objects.equal(req.getParameter(Parameter.BASE64), "1")) {
                payloadWriter = base64PayloadWriter;
            } else {
                payloadWriter = xhr2PayloadWriter;
            }
        }

        public void handle() throws IOException {
            String transport = req.getParameter(Parameter.TRANSPORT);

            String sid = req.getParameter(Parameter.SESSION_ID);
            engineIo.onRequest(transport, sid, new Response() {
                @Override
                public void sendHandshake(EngineIo.HandshakeResponse handshakeResponse) {
                    if (cookie != null) {
                        resp.addCookie(new Cookie(cookie, handshakeResponse.sid));
                    }

                    try {
                        payloadWriter.writePayload(resp,
                                new StringPacket(Type.OPEN, objectMapper.writeValueAsString(handshakeResponse)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void startAsync() {
                    req.startAsync();
                }
            });
        }
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
}
