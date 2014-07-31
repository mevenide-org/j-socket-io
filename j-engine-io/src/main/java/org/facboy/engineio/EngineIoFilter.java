package org.facboy.engineio;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.protocol.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Christopher Ng
 */
public class EngineIoFilter implements Filter {
    private final EngineIo engineIo;
    private final ObjectMapper objectMapper;
    private final String wsEndpointPath;

    @Inject
    public EngineIoFilter(EngineIo engineIo, ObjectMapper objectMapper,
            @Nonnull @Named("engine.io.wsEndpointPath") String wsEndpointPath) {
        this.engineIo = engineIo;
        this.objectMapper = objectMapper;
        this.wsEndpointPath = wsEndpointPath;
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
                engineIo.checkRequest(protocol, transport, sid);

                chain.doFilter(req, resp);
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
        resp.getOutputStream().flush();
    }

    @Override
    public void destroy() {

    }
}
