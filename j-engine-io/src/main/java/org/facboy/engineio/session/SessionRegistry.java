package org.facboy.engineio.session;

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
    private final ConcurrentMap<String, Session> sessions = Maps.newConcurrentMap();

    public void registerSession(Session session) {
        sessions.put(session.getId(), session);
    }

    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Set<String> getSessions() {
        return ImmutableSet.copyOf(sessions.keySet());
    }
}
