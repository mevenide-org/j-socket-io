package org.facboy.engineio.example.chat;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.facboy.engineio.EngineIoServlet;

/**
 * @author Christopher Ng
 */
public class OneServletContext
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");

        servletContextHandler.addServlet(new ServletHolder(new EngineIoServlet()), "/socket.io/*");

        ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
        Resource htmlResource = Resource.newClassPathResource("/static");
        defaultServletHolder.setInitParameter("resourceBase", htmlResource.toString());
        servletContextHandler.addServlet(defaultServletHolder, "/");

        server.setHandler(servletContextHandler);

        server.start();
        server.join();
    }
}
