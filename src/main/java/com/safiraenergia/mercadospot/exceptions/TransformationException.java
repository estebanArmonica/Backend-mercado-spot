package com.safiraenergia.mercadospot.exceptions;

public class TransformationException extends RuntimeException {
    public TransformationException(){
        super();
    }

    // constructor con mensaje
    public TransformationException(String message) {
        super(message);
    }

    // Constructor con mensaje y causa (el que necesitas)
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Constructor con causa
    public TransformationException(Throwable cause) {
        super(cause);
    }
}
