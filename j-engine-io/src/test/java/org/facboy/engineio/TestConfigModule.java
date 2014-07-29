package org.facboy.engineio;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * @author Christopher Ng
 */
public class TestConfigModule extends AbstractModule {
    private final Map<String, Object> config;

    public TestConfigModule(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getValue() instanceof String) {
                bindConstant().annotatedWith(Names.named(entry.getKey())).to((String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                bindConstant().annotatedWith(Names.named(entry.getKey())).to((Integer) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                bindConstant().annotatedWith(Names.named(entry.getKey())).to((Boolean) entry.getValue());
            } else if (entry.getValue() instanceof List) {
                bind(STRING_COLLECTION).annotatedWith(Names.named(entry.getKey())).toInstance((List<String>) entry.getValue());
            } else {
                throw new IllegalArgumentException("Unexpected value class: " + entry.getValue().getClass().getName());
            }
        }
    }

    private static final TypeLiteral<Collection<String>> STRING_COLLECTION = new TypeLiteral<Collection<String>>() {};
}
