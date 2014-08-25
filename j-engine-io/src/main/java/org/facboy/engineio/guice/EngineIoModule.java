package org.facboy.engineio.guice;

import org.facboy.engineio.EngineIo;
import org.facboy.engineio.session.Base64IdGenerator;
import org.facboy.engineio.session.IdGenerator;

import com.google.inject.AbstractModule;

/**
 * @author Christopher Ng
 */
public class EngineIoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EngineIo.class);
        bind(IdGenerator.class).to(Base64IdGenerator.class);
    }
}
