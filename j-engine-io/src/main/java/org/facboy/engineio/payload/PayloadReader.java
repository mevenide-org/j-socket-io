package org.facboy.engineio.payload;

import java.io.InputStream;

import org.facboy.engineio.protocol.Packet;

/**
 * @author Christopher Ng
 */
public interface PayloadReader {
    Iterable<Packet> readPayload(InputStream inputStream);
}
