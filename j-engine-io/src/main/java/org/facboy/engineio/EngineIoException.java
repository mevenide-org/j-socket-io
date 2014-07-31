package org.facboy.engineio;

import org.facboy.engineio.protocol.ProtocolError;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Christopher Ng
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class EngineIoException extends Exception {
    private final int statusCode;
    private final ProtocolError error;

    public EngineIoException(ProtocolError error, int httpStatusCode) {
        super(error.message());
        this.statusCode = httpStatusCode;
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @JsonProperty
    public int getCode() {
        return error.code();
    }

    @JsonProperty
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
