package org.facboy.engineio.protocol;

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
}
