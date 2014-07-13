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
    private final int code;

    public EngineIoException(int statusCode, int code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @JsonProperty
    public int getCode() {
        return code;
    }

    @JsonProperty
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
