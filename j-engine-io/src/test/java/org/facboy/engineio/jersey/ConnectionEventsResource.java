package org.facboy.engineio.jersey;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.facboy.engineio.ConnectionEventCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * @author Christopher Ng
 */
@Path("events/connection")
@Singleton
public class ConnectionEventsResource {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionEventsResource.class);

//    private final ConnectionEventCache connectionEventCache;

    private final Set<AsyncHolder> asyncResponses = Sets.newConcurrentHashSet();

    @Inject
    public ConnectionEventsResource(final ConnectionEventCache connectionEventCache) {
//        engineIo.addEventListener(new DefaultEngineIoEventListener() {
//            @Override
//            public void onConnection(ConnectionEvent event) {
//                for (Iterator<AsyncHolder> i = asyncResponses.iterator(); i.hasNext(); ) {
//                    try {
//                        AsyncHolder asyncHolder = i.next();
//
//                        NavigableMap<?, EventWrapper<ConnectionEvent>> outstandingEvents = asyncHolder.lastId == null ? connectionEvents : connectionEvents.tailMap(asyncHolder.lastId, false);
//                        if (!outstandingEvents.isEmpty()) {
//                            asyncHolder.asyncResponse.resume(ImmutableList.copyOf(outstandingEvents.values()));
//                            i.remove();
//                        }
//                    } catch (Exception e) {
//                        logger.error("Error handling connection event:", e);
//                    }
//                }
//            }
//        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void getEvents(@Suspended final AsyncResponse asyncResponse, @QueryParam("lastId") UUID lastId) {
        asyncResponse.setTimeout(1, TimeUnit.MINUTES);

//        NavigableMap<?, EventWrapper<ConnectionEvent>> outstandingEvents = lastId == null ? connectionEvents : connectionEvents.tailMap(lastId, false);
//        if (outstandingEvents.isEmpty()) {
//            final AsyncHolder asyncHolder = new AsyncHolder(lastId, asyncResponse);
//            asyncResponse.register(new AsyncCallbacks() {
//                @Override
//                public void onComplete(Throwable throwable) {
//                    asyncResponses.remove(asyncHolder);
//                }
//
//                @Override
//                public void onDisconnect(AsyncResponse disconnected) {
//                    asyncResponses.remove(asyncHolder);
//                }
//            });
//            asyncResponses.add(asyncHolder);
//        } else {
//            asyncResponse.resume(ImmutableList.copyOf(outstandingEvents.values()));
//        }
    }

    private static interface AsyncCallbacks extends CompletionCallback, ConnectionCallback {
    }

    private static class AsyncHolder {
        final UUID lastId;
        final AsyncResponse asyncResponse;

        AsyncHolder(UUID lastId, AsyncResponse asyncResponse) {
            this.lastId = lastId;
            this.asyncResponse = asyncResponse;
        }
    }
}
