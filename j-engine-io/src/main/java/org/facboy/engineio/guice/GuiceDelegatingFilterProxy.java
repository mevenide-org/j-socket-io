package org.facboy.engineio.guice;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.facboy.engineio.EngineIoFilter;

import com.google.inject.Injector;

/**
 * @author Christopher Ng
 */
public class GuiceDelegatingFilterProxy implements Filter {
    public static final String GUICE_INJECTOR_ATTRIBUTE = "guiceInjector";

    private EngineIoFilter delegate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Injector injector = (Injector) filterConfig.getServletContext().getAttribute(GUICE_INJECTOR_ATTRIBUTE);
        delegate = injector.getInstance(EngineIoFilter.class);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        delegate.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
        delegate = null;
    }
}
