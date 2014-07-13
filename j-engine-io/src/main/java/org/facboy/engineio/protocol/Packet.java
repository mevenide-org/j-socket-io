package org.facboy.engineio.protocol;

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

        public String getOrdinalString() {
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
}
