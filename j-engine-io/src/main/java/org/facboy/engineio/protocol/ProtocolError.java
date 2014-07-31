package org.facboy.engineio.protocol;

/**
* @author Christopher Ng
*/
public enum ProtocolError {
    UNKNOWN_TRANSPORT("Transport unknown"),
    UNKNOWN_SID("Session ID unknown"),
    BAD_HANDSHAKE_METHOD("Bad handshake method"),
    BAD_REQUEST("Bad request");

    private final String message;

    ProtocolError(String message) {
        this.message = message;
    }

    public int code() {
        return ordinal();
    }

    public String message() {
        return message;
    }
}
