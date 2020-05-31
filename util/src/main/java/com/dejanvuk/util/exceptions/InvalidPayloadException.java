package com.dejanvuk.util.exceptions;

public class InvalidPayloadException extends RuntimeException {
    public InvalidPayloadException() {
    }

    public InvalidPayloadException(String message) {
        super(message);
    }

    public InvalidPayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPayloadException(Throwable cause) {
        super(cause);
    }
}
