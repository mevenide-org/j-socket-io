package org.facboy.engineio.event;

/**
 * @author Christopher Ng
 */
public interface EngineIoEventListener<T> {
    void onEvent(T event);

}
