package org.facboy.engineio.jsapi;

import java.util.Set;

import org.facboy.engineio.EngineIo;
import org.facboy.engineio.session.SessionRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

/**
 * @author Christopher Ng
 */
public class TestEngineIo {
    private final SessionRegistry sessionRegistry;
    private final EngineIo engineIo;
    private final ObjectMapper objectMapper;

    @Inject
    public TestEngineIo(SessionRegistry sessionRegistry, EngineIo engineIo, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.engineIo = engineIo;
        this.objectMapper = objectMapper;
    }

    public Set<String> getClients() {
        return sessionRegistry.getSessions();
    }
}
