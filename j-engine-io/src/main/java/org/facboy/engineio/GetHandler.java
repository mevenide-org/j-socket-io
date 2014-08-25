package org.facboy.engineio;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIo.HandshakeSender;
import org.facboy.engineio.payload.Base64PayloadWriter;
import org.facboy.engineio.payload.PayloadWriter;
import org.facboy.engineio.payload.Xhr2PayloadWriter;
import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.Packet.Type;
import org.facboy.engineio.protocol.Parameter;
import org.facboy.engineio.protocol.StringPacket;
import org.facboy.engineio.session.Session;
import org.facboy.engineio.session.Session.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * TODO move looking up sid/transport etc to common pre-process for Post and Get.
 *
 * @author Christopher Ng
 */
@Singleton
public class GetHandler {
    private static final Logger logger = LoggerFactory.getLogger(GetHandler.class);

    public static final String DEFAULT_COOKIE = "io";

    private final EngineIo engineIo;
    private final Xhr2PayloadWriter xhr2PayloadWriter;
    private final Base64PayloadWriter base64PayloadWriter;
    private final ObjectMapper objectMapper;

    private String cookie = DEFAULT_COOKIE;

    @Inject
    public GetHandler(EngineIo engineIo, Xhr2PayloadWriter xhr2PayloadWriter,
            Base64PayloadWriter base64PayloadWriter, ObjectMapper objectMapper) {
        this.engineIo = engineIo;
        this.xhr2PayloadWriter = xhr2PayloadWriter;
        this.base64PayloadWriter = base64PayloadWriter;
        this.objectMapper = objectMapper;
    }

    public void handle(Session session, final HttpServletRequest req, final HttpServletResponse resp) {
        final PayloadWriter payloadWriter;
        if (Objects.equal(req.getParameter(Parameter.BASE64), "1")) {
            payloadWriter = base64PayloadWriter;
        } else {
            payloadWriter = xhr2PayloadWriter;
        }

        if (session.isHandshake()) {
            engineIo.handshake(session, new HandshakeSender() {
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
            });
        } else {
            // TODO pretty shite.  Probably need to ensure that session is populated with the appropriate kind of sender
            // before it is registered
            final AsyncContext context = req.startAsync();
            session.setSender(new Sender() {
                @Override
                public void send(StringPacket packet) {
                    try {
                        payloadWriter.writePayload((HttpServletResponse) context.getResponse(), packet);
                    } catch (IOException e) {
                        try {
                            ((HttpServletResponse) context.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        } catch (IOException e1) {
                            logger.error("Error", e1);
                        }
                        throw new RuntimeException(e);
                    } finally {
                        context.complete();
                    }
                }

                @Override
                public void send(BinaryPacket packet) {
                    try {
                        payloadWriter.writePayload((HttpServletResponse) context.getResponse(), packet);
                    } catch (IOException e) {
                        try {
                            ((HttpServletResponse) context.getResponse()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        } catch (IOException e1) {
                            logger.error("Error", e1);
                        }
                        throw new RuntimeException(e);
                    } finally {
                        context.complete();
                    }
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
        if (cookieEnabled) {
            this.cookie = DEFAULT_COOKIE;
        } else {
            this.cookie = null;
        }
    }
}
