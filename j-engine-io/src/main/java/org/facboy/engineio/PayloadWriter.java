package org.facboy.engineio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.Packet;
import org.facboy.engineio.protocol.StringPacket;

/**
 * @author Christopher Ng
 */
public class PayloadWriter {
    public void writePayload(OutputStream out, StringPacket packet) throws IOException {
        out.write(0);  // is a string (not true binary = 0)
        writeLength(out, packet.getData().length() + 1); // the type is prepended
        out.write(255);
        out.write(packet.getType().getOrdinalString().getBytes("UTF-8"));
        out.write(packet.getData().getBytes("UTF-8"));
    }

    public void writePayload(OutputStream out, BinaryPacket packet) throws IOException {
        out.write(1); // is binary (true binary = 1)
        writeLength(out, packet.getLength() + 1); // the type is prepended
        out.write(255);
        out.write(packet.getType().ordinal());
        out.write(packet.getData(), packet.getOffset(), packet.getLength());
    }

    private void writeLength(OutputStream out, int length) throws IOException {
        int digits = (int) Math.log10(length) + 1;
        int divisor = 1;
        for (int i = 1; i < digits; i++) {
            divisor *= 10;
        }
        for (int i = digits; i > 1; i--) {
            int digit = length / divisor;
            length = length - digit * divisor;
            divisor /= 10;
            out.write(digit);
        }
        out.write(length);
    }
}
