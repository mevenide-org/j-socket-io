package org.facboy.engineio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.facboy.engineio.guice.EngineIoModule;
import org.facboy.engineio.session.SessionRegistry;
import org.facboy.engineio.jsapi.TestEngineIo;
import org.facboy.engineio.jsapi.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * @author Christopher Ng
 */
public class TestServer {
    private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

    public static final String ENGINE_IO_PATH = "/engine.io";

    public static void main(String[] args) throws Exception {
        TestServer server = new TestServer(8081, ImmutableMap.<String, Object>of());
        server.start();
        server.server.join();
    }

    private final DefaultServlet defaultServlet = new DefaultServlet();

    private final Integer port;
    private final Map<String, Object> config;

    private Injector injector;
    private Server server;

    public TestServer() {
        this(ImmutableMap.<String, Object>of());
    }

    public TestServer(Map<String, Object> config) {
        this(null, config);
    }

    public TestServer(Integer port, Map<String, Object> config) {
        this.port = port;
        this.config = config;
    }

    public int start() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        if (port != null) {
            connector.setPort(port);
        }
        server.setConnectors(new Connector[] {connector});

        server.setHandler(createServletContextHandler(config));

        server.start();

        return connector.getLocalPort();
    }

    private ServletContextHandler createServletContextHandler(Map<String, Object> config) {
        injector = Guice.createInjector(
                new EngineIoModule(),
                new TestConfigModule(config)
//                new ServletModule() {
//                    @Override
//                    protected void configureServlets() {
//                        serve(ENGINE_IO_PATH + "/*").with(EngineIoServlet.class);
//                    }
//                }
        );

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");

        FilterHolder filterHolder = new FilterHolder(injector.getInstance(EngineIoFilter.class));
        filterHolder.setAsyncSupported(true);
        servletContextHandler.addFilter(filterHolder, ENGINE_IO_PATH + "/*", EnumSet.allOf(DispatcherType.class));

        addWebsocketFilter(servletContextHandler, injector);

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

        addSpringStomp(servletContextHandler, injector);

        return servletContextHandler;
    }

    private void addWebsocketFilter(ServletContextHandler context, final Injector injector) {
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
            jettyContainer.addEndpoint(ServerEndpointConfig.Builder.create(EngineIoEndpoint.class, ENGINE_IO_PATH + "/")
                    .configurator(new Configurator() {
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                            return injector.getInstance(endpointClass);
                        }
                    })
                    .build());
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSpringStomp(ServletContextHandler context, final Injector injector) {
        ServletHolder springWebsocketServletHolder = new ServletHolder("websocketRequestHandler", HttpRequestHandlerServlet.class);
        context.addServlet(springWebsocketServletHolder, "/springws/*");

        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(WebSocketConfig.class);
        ctx.scan("org.facboy.engineio.jsapi");
        ctx.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                beanFactory.registerSingleton("injector", injector);

                List<Class<?>> classes =  ImmutableList.of(EngineIo.class, SessionRegistry.class);
                for (Class<?> cls : classes) {
                    beanFactory.registerResolvableDependency(cls, injector.getInstance(cls));
                }
            }
        });
        context.addEventListener(new ContextLoaderListener(ctx));
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
            injector = null;
        }
    }

    public TestEngineIo getTestEngineIo() {
        checkNotNull(injector, "injector is null");
        return injector.getInstance(TestEngineIo.class);
    }
}
