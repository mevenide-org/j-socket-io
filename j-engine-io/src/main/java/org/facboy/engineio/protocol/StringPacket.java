package org.facboy.engineio.protocol;

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
}
