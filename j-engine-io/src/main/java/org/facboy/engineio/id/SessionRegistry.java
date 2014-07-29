package org.facboy.engineio.id;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

/**
 * @author Christopher Ng
 */
@Singleton
public class SessionRegistry {
    private final ConcurrentMap<String, Boolean> sessions = Maps.newConcurrentMap();

    public void registerSession(String sessionId) {
        sessions.put(sessionId, true);
    }

    public Boolean getSession(String session) {
        return sessions.get(session);
    }

    public Set<String> getSessions() {
        return ImmutableSet.copyOf(sessions.keySet());
    }
}
