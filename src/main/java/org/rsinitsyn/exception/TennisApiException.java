package org.rsinitsyn.exception;

import lombok.Getter;

public class TennisApiException extends RuntimeException {
    @Getter
    private final int code;

    public TennisApiException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public TennisApiException(String message, Throwable cause) {
        this(message, cause, 500);
    }

    public TennisApiException(String message, int code) {
        this(message, new RuntimeException(), code);
    }

    public TennisApiException(String message) {
        this(message, new RuntimeException());
    }
}
