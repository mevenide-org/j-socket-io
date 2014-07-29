package org.facboy.engineio;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.facboy.engineio.guice.EngineIoModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

/**
 * @author Christopher Ng
 */
public class TestServer {
    private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

    public static void main(String[] args) throws Exception {
        TestServer server = new TestServer();
        server.start();
    }

    private final DefaultServlet defaultServlet = new DefaultServlet();
    private final TestServerConfigurer testServerConfigurer = new DynamicTestServerConfigurer();

    private Server server;
    private HandlerCollection handlerCollection;

    private TestServer() {
    }

    private void start() throws Exception {
        server = new Server(8081);

        handlerCollection = new HandlerCollection(true);
        handlerCollection.setHandlers(new Handler[] {createServletContextHandler(ImmutableMap.<String, Object>of())});
        server.setHandler(handlerCollection);

        server.start();
        server.join();
    }

    private ServletContextHandler createServletContextHandler(Map<String, Object> config) {
        final Injector injector = Guice.createInjector(
                new EngineIoModule(),
                new TestConfigModule(config),
                new ServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve("/engine.io/*").with(EngineIoServlet.class);
                        serve("/engine.io.manage/*").with(EngineIoTestManager.class);

                        bind(TestServerConfigurer.class).toInstance(testServerConfigurer);

                        bindConstant().annotatedWith(Names.named("engine.io.wsEndpointPath")).to("/engine.io.ws");
                    }
                }
        );

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
//        webAppContext.setConfigurationClasses(ImmutableList.<String>builder()
//                .add(WebInfConfiguration.class.getName())
//                .add(AnnotationConfiguration.class.getName())
//                .build());
//        webAppContext.getMetaData().addContainerResource(Resource.newResource(new File("target/classes")));
//        webAppContext.getMetaData().addContainerResource(Resource.newResource(new File("target/test-classes")));
        servletContextHandler.setResourceBase("/null");
        servletContextHandler.setContextPath("/");

        FilterHolder filterHolder = new FilterHolder(injector.getInstance(EngineIoFilter.class));
        filterHolder.setAsyncSupported(true);
        servletContextHandler.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));

        addWebsocketFilter(servletContextHandler);

        filterHolder = new FilterHolder(GuiceFilter.class);
        filterHolder.setAsyncSupported(true);
        servletContextHandler.addFilter(filterHolder, "/*", EnumSet.allOf(DispatcherType.class));

        ServletHolder defaultServletHolder = new ServletHolder(defaultServlet);
        servletContextHandler.addServlet(defaultServletHolder, "/");

        servletContextHandler.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        });


        return servletContextHandler;
    }

    private void addWebsocketFilter(ServletContextHandler context) {
        // Create Filter
        WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configureContext(context);

        // Store reference to the WebSocketUpgradeFilter
        context.setAttribute(WebSocketUpgradeFilter.class.getName(),filter);

        // Create the Jetty ServerContainer implementation
        org.eclipse.jetty.websocket.jsr356.server.ServerContainer jettyContainer = new org.eclipse.jetty.websocket.jsr356.server.ServerContainer(
                filter,
                filter.getFactory(),
                server.getThreadPool());
        context.addBean(jettyContainer);

        // Store a reference to the ServerContainer per javax.websocket spec 1.0 final section 6.4 Programmatic Server Deployment
        context.setAttribute(javax.websocket.server.ServerContainer.class.getName(), jettyContainer);

        try {
            jettyContainer.addEndpoint(ServerEndpointConfig.Builder.create(EngineIoEndpoint.class, "/engine.io/").build());
        } catch (DeploymentException e) {
            throw new RuntimeException(e);  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private class DynamicTestServerConfigurer implements TestServerConfigurer {
        @Override
        public void reconfigureServer(Map<String, Object> config) {
            // TODO replacing the entire context handler each time is not perhaps the most efficient way of doing
            // this, but it works pretty reliably.  replacing the injector is quite fiddly otherwise, you
            // need to set the new injector in the servletcontext, then somehow make the GuiceFilter reinitialize itself
            ServletContextHandler newServletContextHandler = createServletContextHandler(config);
            handlerCollection.setHandlers(new Handler[] {newServletContextHandler});
            try {
                newServletContextHandler.start();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}