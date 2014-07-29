package org.facboy.engineio;

/**
 * @author Christopher Ng
 */
public interface EngineIoRequest {
    String getParameter(String name);

    void startAsync();
}
