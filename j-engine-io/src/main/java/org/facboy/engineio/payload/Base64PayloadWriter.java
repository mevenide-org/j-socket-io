package org.facboy.engineio.payload;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

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
    private static final byte[] COLON_BYTES;
    private static final byte[] B_BYTES;
    static {
        try {
            B_BYTES = "b".getBytes("UTF-8");
            COLON_BYTES = ":".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void writePayload(HttpServletResponse resp, StringPacket packet) throws IOException {
        setContentType(resp);
        OutputStream out = resp.getOutputStream();

        writeLength(out, packet.getData().length() + 1); // the type is prepended
        out.write(packet.getType().ordinalString().getBytes("UTF-8"));
        out.write(packet.getData().getBytes("UTF-8"));
    }

    @Override
    public void writePayload(HttpServletResponse resp, BinaryPacket packet) throws IOException {
        setContentType(resp);
        OutputStream out = resp.getOutputStream();

        String base64Encoded = BaseEncoding.base64().encode(packet.getData(), packet.getOffset(), packet.getLength());

        writeLength(out, base64Encoded.length() + 1); // the type is prepended
        out.write(B_BYTES);
        out.write(packet.getType().ordinalString().getBytes("UTF-8"));
        out.write(base64Encoded.getBytes("UTF-8"));
    }

    private void setContentType(HttpServletResponse resp) {
        resp.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString());
    }

    private void writeLength(OutputStream out, int length) throws IOException {
        out.write(Integer.toString(length).getBytes("UTF-8"));
        out.write(COLON_BYTES);
    }
}
