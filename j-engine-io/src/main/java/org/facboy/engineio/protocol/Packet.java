package org.facboy.engineio.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * @author Christopher Ng
 */
public abstract class Packet {
    /**
     * The order is significant!
     */
    public enum Type {
        OPEN,
        CLOSE,
        PONG,
        MESSAGE,
        UPGRADE,
        NOOP;

        final String ordinalString = Integer.toString(ordinal());

        public String ordinalString() {
            return ordinalString;
        }
    }

    private final Type type;

    protected Packet(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract void write(OutputStream out) throws IOException;
    
    public abstract void write(Writer writer) throws IOException;
}
