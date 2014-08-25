package org.facboy.engineio.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import com.google.common.base.Objects;

/**
 * @author Christopher Ng
 */
public class BinaryPacket extends Packet {
    private final byte[] data;
    private final int offset;
    private final int length;

    public BinaryPacket(Type type, byte[] data, int offset, int length) {
        super(type);
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        out.write(getType().ordinal());
        out.write(data, offset, length);
    }

    @Override
    public void write(Writer writer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("type", getType())
                .add("data", Arrays.toString(data))
                .add("offset", offset)
                .add("length", length)
                .toString();
    }
}
