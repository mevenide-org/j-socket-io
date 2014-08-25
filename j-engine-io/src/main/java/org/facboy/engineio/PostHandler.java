package org.facboy.engineio;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.payload.Base64PayloadReader;
import org.facboy.engineio.protocol.Packet;
import org.facboy.engineio.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author Christopher Ng
 */
@Singleton
public class PostHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostHandler.class);

    public static final String DEFAULT_COOKIE = "io";

    private final EngineIo engineIo;
    private final Base64PayloadReader base64PayloadReader;
    private final ObjectMapper objectMapper;

    private String cookie = DEFAULT_COOKIE;

    @Inject
    public PostHandler(EngineIo engineIo,
            Base64PayloadReader base64PayloadReader, ObjectMapper objectMapper) {
        this.engineIo = engineIo;
        this.base64PayloadReader = base64PayloadReader;
        this.objectMapper = objectMapper;
    }

    public void handle(Session session, final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            for (Packet packet : base64PayloadReader.readPayload(req.getInputStream())) {
                switch (packet.getType()) {
                    case MESSAGE:
                        engineIo.onMessage(session, packet);
                    case PING:
                        // TODO handle ping
                        break;
                    default:
                        logger.info("received unhandled packet: {}", packet);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
