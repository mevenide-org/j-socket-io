package org.facboy.engineio.payload;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;

/**
 * @author Christopher Ng
 */
public interface PayloadWriter {
    void writePayload(HttpServletResponse resp, StringPacket packet) throws IOException;

    void writePayload(HttpServletResponse resp, BinaryPacket packet) throws IOException;
}
