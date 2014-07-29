package org.facboy.engineio.payload;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

/**
 * @author Christopher Ng
 */
public class Xhr2PayloadWriter implements PayloadWriter {
    @Override
    public void writePayload(HttpServletResponse resp, StringPacket packet) throws IOException {
        setContentType(resp);
        OutputStream out = resp.getOutputStream();

        out.write(0);  // is a string (not true binary = 0)
        writeLength(out, packet.getData().length() + 1); // the type is prepended
        out.write(packet.getType().ordinalString().getBytes("UTF-8"));
        out.write(packet.getData().getBytes("UTF-8"));
    }

    @Override
    public void writePayload(HttpServletResponse resp, BinaryPacket packet) throws IOException {
        setContentType(resp);
        OutputStream out = resp.getOutputStream();

        out.write(1); // is binary (true binary = 1)
        writeLength(out, packet.getLength() + 1); // the type is prepended
        out.write(packet.getType().ordinal());
        out.write(packet.getData(), packet.getOffset(), packet.getLength());
    }

    private void setContentType(HttpServletResponse resp) {
        resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
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
        out.write(255);
    }
}
