package org.facboy.engineio;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.protocol.Parameter;
import org.facboy.engineio.session.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Christopher Ng
 */
@Singleton
public class EngineIoFilter implements Filter {
    private final EngineIo engineIo;

    private final GetHandler getHandler;
    private final PostHandler postHandler;
    private final WebsocketHandler websocketHandler;

    private final ObjectMapper objectMapper;

    @Inject
    public EngineIoFilter(EngineIo engineIo, GetHandler getHandler, PostHandler postHandler,
            WebsocketHandler websocketHandler,
            ObjectMapper objectMapper) {
        this.engineIo = engineIo;
        this.getHandler = getHandler;
        this.postHandler = postHandler;
        this.websocketHandler = websocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            String protocol = req.getParameter(Parameter.PROTOCOL);
            String transport = req.getParameter(Parameter.TRANSPORT);
            String sid = req.getParameter(Parameter.SESSION_ID);

            setOriginHeaders(req, resp);

            try {
                Session session = engineIo.getSession(protocol, transport, sid);

                // handle http GET
                if ("GET".equals(req.getMethod())) {
                    if (websocketHandler.isUpgradeRequest(req)) {
                        // let this fall through to the websocket implementation in the container (on jetty, who knows what tomcat does)
                        // TODO will this work for tomcat?
                        chain.doFilter(request, response);
                    } else {
                        getHandler.handle(session, req, resp);
                    }
                } else if ("POST".equals(req.getMethod())) {
                    postHandler.handle(session, req, resp);
                } else {
                    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
                }
            } catch (EngineIoException e) {
                handleError(resp, e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void setOriginHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String originHeader = req.getHeader(HttpHeaders.ORIGIN);
        if (Strings.isNullOrEmpty(originHeader)) {
            resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, originHeader);
            resp.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    private void handleError(HttpServletResponse resp, EngineIoException engineIoException) throws IOException {
        resp.setStatus(engineIoException.getStatusCode());
        resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        objectMapper.writeValue(resp.getOutputStream(), engineIoException);
    }

    @Override
    public void destroy() {
    }
}
