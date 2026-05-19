package com.safiraenergia.mercadospot.enums;

public enum ETLStatus {
    STARTED,
    VALIDATING,
    EXTRACTING,
    TRANSFORMING,
    LOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    NOT_FOUND
}
