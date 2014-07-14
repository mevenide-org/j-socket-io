package org.facboy.engineio;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

/**
 * @author Christopher Ng
 */
public class TestServletContext
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8081);

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");

        servletContextHandler.addServlet(new ServletHolder(new EngineIoServlet()), "/engine.io/*");

        ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
        servletContextHandler.addServlet(defaultServletHolder, "/");

        server.setHandler(servletContextHandler);

        server.start();
        server.join();
    }
}
