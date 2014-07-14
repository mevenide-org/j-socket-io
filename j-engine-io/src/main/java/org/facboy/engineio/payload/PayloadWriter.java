package org.facboy.engineio.payload;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;

/**
 * @author Christopher Ng
 */
public interface PayloadWriter {
    void writePayload(HttpServletResponse resp, StringPacket packet) throws IOException;

    void writePayload(HttpServletResponse resp, BinaryPacket packet) throws IOException;
}
