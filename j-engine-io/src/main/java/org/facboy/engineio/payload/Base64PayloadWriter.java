package org.facboy.engineio.payload;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.facboy.engineio.protocol.BinaryPacket;
import org.facboy.engineio.protocol.StringPacket;

import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

/**
 * @author Christopher Ng
 */
public class Base64PayloadWriter implements PayloadWriter {
    @Override
    public void writePayload(HttpServletResponse resp, StringPacket packet) throws IOException {
        setContentType(resp);
        Writer writer = resp.getWriter();

        writeLength(writer, packet.getData().length() + 1); // the type is prepended
        packet.write(writer);
    }

    @Override
    public void writePayload(HttpServletResponse resp, BinaryPacket packet) throws IOException {
        setContentType(resp);
        Writer writer = resp.getWriter();

        String base64Encoded = BaseEncoding.base64().encode(packet.getData(), packet.getOffset(), packet.getLength());

        writeLength(writer, base64Encoded.length() + 1); // the type is prepended
        writer.write('b');
        writer.write(packet.getType().ordinalString());
        packet.write(BaseEncoding.base64().encodingStream(writer));
    }

    private void setContentType(HttpServletResponse resp) {
        resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString());
        resp.setCharacterEncoding("UTF-8");
    }

    private void writeLength(Writer writer, int length) throws IOException {
        writer.write(Integer.toString(length));
        writer.write(":");
    }
}
