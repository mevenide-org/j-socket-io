package org.facboy.engineio;

import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import com.google.common.collect.ImmutableSet;

/**
 * @author Christopher Ng
 */
public class TestServerApplicationConfig implements ServerApplicationConfig {
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return ImmutableSet.of(ServerEndpointConfig.Builder.create(EngineIoEndpoint.class, "/engine.io").build());
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return ImmutableSet.of();
    }
}
