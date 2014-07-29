package org.facboy.engineio;

import java.util.Map;

/**
 * @author Christopher Ng
 */
public interface TestServerConfigurer {
    void reconfigureServer(Map<String, Object> config);
}
