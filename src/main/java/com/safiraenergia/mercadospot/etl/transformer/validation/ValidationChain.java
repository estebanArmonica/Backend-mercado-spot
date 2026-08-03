package com.safiraenergia.mercadospot.etl.transformer.validation;

import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.exceptions.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ValidationChain {
    private Validator chain;

    public ValidationChain() {
        buildChain();
    }

    private void buildChain(){
        Validator notNullValidator = new NotNullValidator();
        Validator folioValidator = new FolioValidator();
        Validator montoValidator = new MontoValidator();
        Validator rutValidator = new RutValidator();
        Validator fechaValidator = new FechaValidator();

        notNullValidator.setNext(folioValidator)
                       .setNext(montoValidator)
                       .setNext(rutValidator)
                       .setNext(fechaValidator);
        
        this.chain = notNullValidator;
    }

    public void validate(FacturaDTO dto) throws ValidationException {
        chain.validate(dto);
    }
}

// sub clase
class NotNullValidator implements Validator {
    private Validator next;
    
    @Override
    public void validate(FacturaDTO dto) throws ValidationException {
        if (dto.getFolio() == null || dto.getFolio() <= 0) {
            throw new ValidationException("Folio cannot be null or zero");
        }
        if (dto.getMontoNeto() == 0) {
            throw new ValidationException("Monto neto cannot be zero");
        }
        if (dto.getRutEntidad() == null || dto.getRutEntidad().isEmpty()) {
            throw new ValidationException("RUT cannot be null or empty");
        }

        // No valida el estado, por defecto lo deja en PENDIENTE
        if (next != null) next.validate(dto);
    }
    
    @Override
    public Validator setNext(Validator next) {
        this.next = next;
        return next;
    }
}

class FolioValidator implements Validator {
    private Validator next;
    
    @Override
    public void validate(FacturaDTO dto) throws ValidationException {
        if (dto.getFolio() <= 0) {
            throw new ValidationException("Folio must be greater than 0: " + dto.getFolio());
        }
        if (next != null) next.validate(dto);
    }
    
    @Override
    public Validator setNext(Validator next) {
        this.next = next;
        return next;
    }
}

@Slf4j
class MontoValidator implements Validator {
    private Validator next;
    
    @Override
    public void validate(FacturaDTO dto) throws ValidationException {
        if (dto.getMontoNeto() < 0 || dto.getMontoBruto() < 0 || dto.getMontoTotal() < 0) {
            throw new ValidationException("Monto values must be positive");
        }
        // Validate calculation
        double expectedTotal = dto.getMontoNeto() * 1.19; // Assuming 19% IVA
        double actualTotal = dto.getMontoTotal();
        double tolerance = 1.0;
        
        if (Math.abs(actualTotal - expectedTotal) > tolerance) {
            log.warn("Monto total validation warning - Expected: {}, Actual: {}", dto.getFolio(), expectedTotal, dto.getMontoTotal());
        }
        
        if (next != null) next.validate(dto);
    }
    
    @Override
    public Validator setNext(Validator next) {
        this.next = next;
        return next;
    }
}

class RutValidator implements Validator {
    private Validator next;
    
    @Override
    public void validate(FacturaDTO dto) throws ValidationException {
        if (!isValidRut(dto.getRutEntidad())) {
            throw new ValidationException("Invalid RUT format: " + dto.getRutEntidad());
        }
        if (next != null) next.validate(dto);
    }
    
    private boolean isValidRut(String rut) {
        // Implement RUT validation logic
        String rutPattern = "^\\d{1,8}-[\\dkK]$";
        return rut != null && rut.matches(rutPattern);
    }
    
    @Override
    public Validator setNext(Validator next) {
        this.next = next;
        return next;
    }
}

class FechaValidator implements Validator {
    private Validator next;
    
    @Override
    public void validate(FacturaDTO dto) throws ValidationException {
        if (dto.getFechaEmision() != null && dto.getFechaPago() != null) {
            if (dto.getFechaPago().before(dto.getFechaEmision())) {
                throw new ValidationException("Payment date cannot be before emission date");
            }
        }
        if (next != null) next.validate(dto);
    }
    
    @Override
    public Validator setNext(Validator next) {
        this.next = next;
        return next;
    }
}