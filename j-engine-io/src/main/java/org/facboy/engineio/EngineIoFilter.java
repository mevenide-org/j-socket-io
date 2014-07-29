package org.facboy.engineio;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Christopher Ng
 */
public class EngineIoFilter implements Filter {
    private final EngineIo engineIo;
    private final String wsEndpointPath;

    @Inject
    public EngineIoFilter(EngineIo engineIo, @Nonnull @Named("engine.io.wsEndpointPath") String wsEndpointPath) {
        this.engineIo = engineIo;
        this.wsEndpointPath = wsEndpointPath;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        // redirect to the websocket endpoint if this is a websocket upgrade
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
