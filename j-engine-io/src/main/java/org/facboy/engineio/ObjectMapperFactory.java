package org.facboy.engineio;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christopher Ng
 */
public class ObjectMapperFactory {
    private static final ObjectMapperFactory INSTANCE = new ObjectMapperFactory();

    public static ObjectMapperFactory getInstance() {
        return INSTANCE;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
