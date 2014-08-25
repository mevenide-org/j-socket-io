package org.facboy.engineio.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.google.common.base.Objects;

/**
 * @author Christopher Ng
 */
public class StringPacket extends Packet {
    private final String data;

    public StringPacket(Type type, String data) {
        super(type);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        out.write(getType().ordinalString().getBytes("UTF-8"));
        out.write(getData().getBytes("UTF-8"));
    }

    @Override
    public void write(Writer writer) throws IOException {
        writer.write(getType().ordinalString());
        writer.write(getData());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("type", getType())
                .add("data", data)
                .toString();
    }
}
