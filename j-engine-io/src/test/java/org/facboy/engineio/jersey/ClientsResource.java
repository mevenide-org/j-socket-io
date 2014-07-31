package org.facboy.engineio.jersey;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.facboy.engineio.id.SessionRegistry;

/**
 * @author Christopher Ng
 */
@Path("clients")
public class ClientsResource {
    @Inject
    private SessionRegistry sessionRegistry;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getClients() {
        return sessionRegistry.getSessions();
    }
}
