package org.facboy.engineio;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
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
            // prefix with engine.io
            String key;
            if (Objects.equal(entry.getKey(), "cookie") && entry.getValue() instanceof Boolean) {
                key = "engine.io.cookieEnabled";
            } else {
                key = "engine.io." + entry.getKey();
            }

            if (entry.getValue() instanceof String) {
                bindConstant().annotatedWith(Names.named(key)).to((String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                bindConstant().annotatedWith(Names.named(key)).to((Integer) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                bindConstant().annotatedWith(Names.named(key)).to((Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Object[]) {
                // bleh, guice will only bind to the correct type, in some cases we want a collection of strings
                Object[] objs = (Object[]) entry.getValue();
                boolean strings = true;
                for (Object obj : objs) {
                    if (!(obj instanceof String)) {
                        strings = false;
                        break;
                    }
                }

                if (strings) {
                    List raw = ImmutableList.copyOf(objs);
                    bind(STRING_COLLECTION).annotatedWith(Names.named(key)).toInstance(raw);
                } else {
                    bind(OBJECT_COLLECTION).annotatedWith(Names.named(key)).toInstance(ImmutableList.copyOf((Object[]) entry
                            .getValue()));
                }
            } else {
                throw new IllegalArgumentException("Key '" + entry.getKey() + "' has unexpected value class: " + entry.getValue().getClass().getName());
            }
        }
    }

    private static final TypeLiteral<Collection<Object>> OBJECT_COLLECTION = new TypeLiteral<Collection<Object>>() {};
    private static final TypeLiteral<Collection<String>> STRING_COLLECTION = new TypeLiteral<Collection<String>>() {};
}
