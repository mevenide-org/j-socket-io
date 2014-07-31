package org.facboy.engineio.jersey;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Injector;

/**
 * @author Christopher Ng
 */
public class EngineIoTestApplication extends ResourceConfig {
    public static final String GUICE_INJECTOR = "engine.io.guiceInjector";

    @Inject
    public EngineIoTestApplication(ServiceLocator serviceLocator, ServletContext servletContext) {
        packages(EngineIoTestApplication.class.getPackage().getName());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        register(new JacksonJsonProvider(objectMapper));

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);

        Injector injector = (Injector) servletContext.getAttribute(GUICE_INJECTOR);
        checkNotNull(injector, "No '%s' attribute on ServletContext");
        guiceBridge.bridgeGuiceInjector(injector);
    }
}
