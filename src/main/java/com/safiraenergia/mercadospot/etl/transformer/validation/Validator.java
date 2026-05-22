package com.safiraenergia.mercadospot.etl.transformer.validation;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.exceptions.ValidationException;

public interface Validator {
    void validate(FacturaDTO dto) throws ValidationException;
    Validator setNext(Validator next);
}
