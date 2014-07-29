package org.facboy.engineio;

import java.util.Set;

import org.facboy.engineio.id.SessionRegistry;

import com.google.common.collect.Sets;

/**
 * @author Christopher Ng
 */
public class TestSessionRegistry extends SessionRegistry {
    private final Set<SessionListener> sessionListeners = Sets.newConcurrentHashSet();

    @Override
    public void registerSession(String sessionId) {
        super.registerSession(sessionId);
    }

    public interface SessionListener {

    }
}
