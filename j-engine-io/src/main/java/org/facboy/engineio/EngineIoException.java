package org.facboy.engineio;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Christopher Ng
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class EngineIoException extends Exception {
    private final int statusCode;
    private final EngineIo.Error error;

    public EngineIoException(int statusCode, EngineIo.Error error) {
        super(error.message());
        this.statusCode = statusCode;
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
