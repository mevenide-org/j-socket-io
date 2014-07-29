package org.facboy.engineio.payload;

import java.io.IOException;

import org.facboy.engineio.EngineIoResponse;
import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;

/**
 * @author Christopher Ng
 */
public interface PayloadWriter {
    void writePayload(EngineIoResponse resp, StringPacket packet) throws IOException;

    void writePayload(EngineIoResponse resp, BinaryPacket packet) throws IOException;
}
