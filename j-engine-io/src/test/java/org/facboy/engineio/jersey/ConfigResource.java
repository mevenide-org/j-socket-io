package org.facboy.engineio.jersey;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.facboy.engineio.TestServerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author Christopher Ng
 */
@Path("config")
public class ConfigResource {
    private static final Logger logger = LoggerFactory.getLogger(ConfigResource.class);

    @Inject
    private TestServerConfigurer testServerConfigurer;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configure(Map<String, Object> config) {
        // prefix with engine.io
        Map<String, Object> alteredConfig = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (Objects.equal(entry.getKey(), "cookie") && entry.getValue() instanceof Boolean) {
                alteredConfig.put("engine.io.cookieEnabled", entry.getValue());
            } else {
                alteredConfig.put("engine.io." + entry.getKey(), entry.getValue());
            }
        }

        ResponseBuilder response = Response.serverError();
        try {
            testServerConfigurer.reconfigureServer(alteredConfig);
            response = Response.ok();
        } catch (Throwable t) {
            handleReconfigureError(t);
        }
        return response.build();
    }

    private void handleReconfigureError(Throwable t) {
        try {
            logger.error("", t);
        } finally {
            // reconfigure to default
            try {
                testServerConfigurer.reconfigureServer(ImmutableMap.<String, Object>of());
            } catch (Throwable t2) {
                logger.error("", t2);
            }
        }
    }
}
