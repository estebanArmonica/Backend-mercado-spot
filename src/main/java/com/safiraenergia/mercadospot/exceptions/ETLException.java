package com.safiraenergia.mercadospot.exceptions;

public class ETLException extends RuntimeException {
    public ETLException(String message) {
        super(message);
    }
    
    public ETLException(String message, Throwable cause) {
        super(message, cause);
    }
}
