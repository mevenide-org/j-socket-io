package org.facboy.engineio;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.EngineIoEventListener.ConnectionEvent;
import org.facboy.engineio.id.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Christopher Ng
 */
@Singleton
public class EngineIoTestManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EngineIoTestManager.class);

    private static final String EVENT_NUMBER = "engine.io.lastConnectionEventNum";

    private final SessionRegistry sessionRegistry;
    private final TestServerConfigurer testServerConfigurer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<AsyncContext> asyncContexts = Sets.newConcurrentHashSet();
    private final List<ConnectionEvent> connectionEvents = new CopyOnWriteArrayList<ConnectionEvent>();

    @Inject
    public EngineIoTestManager(EngineIo engineIo, SessionRegistry sessionRegistry, TestServerConfigurer testServerConfigurer) {
        this.sessionRegistry = sessionRegistry;
        this.testServerConfigurer = testServerConfigurer;

        objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);

        engineIo.addEventListener(new EngineIoEventListener() {
            @Override
            public void onConnection(ConnectionEvent event) {
                connectionEvents.add(event);
                for (Iterator<AsyncContext> i = asyncContexts.iterator(); i.hasNext(); ) {
                    try {
                        AsyncContext asyncContext = i.next();
                        HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
                        int nextNum = (Integer) asyncContext.getRequest().getAttribute(EVENT_NUMBER);
                        if (connectionEvents.size() > nextNum) {
                            writeConnectionEvent(resp, connectionEvents.get(nextNum));
                            i.remove();
                        }
                    } catch (Exception e) {
                        logger.error("Error handling connection event:", e);
                    }
                }
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (Objects.equal(req.getPathInfo(), "/clients")) {
            resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
            objectMapper.writeValue(resp.getOutputStream(), sessionRegistry.getSessions());
        } else if (Objects.equal(req.getPathInfo(), "/events/connection")) {
            handleConnectionEventListener(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleConnectionEventListener(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int nextNum = getNumber(req.getHeader(EVENT_NUMBER)) + 1;

        if (connectionEvents.size() > nextNum) {
            writeConnectionEvent(resp, connectionEvents.get(nextNum));
        } else {
            req.setAttribute(EVENT_NUMBER, nextNum);
            AsyncContext asyncContext = req.startAsync();
            asyncContexts.add(asyncContext);
        }
    }

    private int getNumber(String numStr) {
        if (Strings.isNullOrEmpty(numStr)) {
            return -1;
        }
        return Integer.parseInt(numStr);
    }

    private void writeConnectionEvent(HttpServletResponse resp, ConnectionEvent event) throws IOException {
        resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        objectMapper.writeValue(resp.getOutputStream(), ImmutableMap.of(
                "sessionId", event.getSessionId(),
                "transport", ImmutableMap.of("name", event.getTransport().name().toLowerCase())
        ));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (Objects.equal(req.getPathInfo(), "/config")) {
            Map<String, Object> config = ImmutableMap.of();
            if (req.getContentLength() > 0) {
                //noinspection unchecked
                config = objectMapper.readValue(req.getInputStream(), Map.class);
            }

            // prefix with engine.io
            Map<String, Object> alteredConfig = Maps.newHashMap();
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                if (Objects.equal(entry.getKey(), "cookie") && entry.getValue() instanceof Boolean) {
                    alteredConfig.put("engine.io.cookieEnabled", entry.getValue());
                } else {
                    alteredConfig.put("engine.io." + entry.getKey(), entry.getValue());
                }
            }

            try {
                testServerConfigurer.reconfigureServer(alteredConfig);
            } catch (Error e) {
                try {
                    handleReconfigureError(resp, e);
                } catch (Throwable t) {
                    logger.error("", t);
                }
                throw e;
            } catch (Exception e) {
                handleReconfigureError(resp, e);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleReconfigureError(HttpServletResponse resp, Throwable t) throws IOException {
        try {
            logger.error("", t);
        } finally {
            // reconfigure to default
            try {
                testServerConfigurer.reconfigureServer(ImmutableMap.<String, Object>of());
            } catch (Throwable t2) {
                logger.error("", t2);
            } finally {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
