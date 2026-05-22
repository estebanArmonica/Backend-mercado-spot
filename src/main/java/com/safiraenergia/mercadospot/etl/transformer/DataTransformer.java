package com.safiraenergia.mercadospot.etl.transformer;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.etl.transformer.validation.ValidationChain;
import com.safiraenergia.mercadospot.exceptions.TransformationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataTransformer {
    
    @Autowired
    private ValidationChain validationChain;

    public List<FacturaDTO> transform(List<Map<String, Object>> rowData) throws TransformationException {
        List<FacturaDTO> factura = new ArrayList<>();

        if(rowData == null || rowData.isEmpty()){
            log.warn("No data to transform");
            return factura;
        }

        for (Map<String, Object> row: rowData) {
            try {
                FacturaDTO dto = transformRow(row);
                validationChain.validate(dto);
                factura.add(dto);
            } catch (Exception e) {
                log.error("Error transforming row: {}", row, e);
                throw new TransformationException("Failed to transform row data", e);
            }
        }

        log.info("Transformed {} records", factura.size());
        return factura;
    }

    private FacturaDTO transformRow(Map<String, Object> row) {
        return FacturaDTO.builder()
            .folio(convertToLong(row.get("folio")))
            .montoNeto(convertToInteger(row.get("monto_neto")))
            .montoBruto(convertToInteger(row.get("monto_bruto")))
            .montoTotal(convertToInteger(row.get("monto_total")))
            .fechaEmision(convertToDate(row.get("fecha_emision")))
            .fechaPago(convertToDate(row.get("fecha_pago")))
            .rutEntidad(convertToString(row.get("rut_entidad")))
            .nomEntidad(convertToString(row.get("nomentidad")))
            .glosa(convertToString(row.get("glosa")))
            .periodo(convertToDate(row.get("periodo")))
            .build();
    }

    private Long convertToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to convert '{}' to Long ", value);
                return null;
            }
        }
        return null;
    }
    
    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (Exception e) {
                log.warn("Falied to convert '{}' to Integer", value);
                return 0;
            }
        }
        return 0;
    }
    
    private String convertToString(Object value) {
        if(value == null) return null;
        return value.toString().trim();
    }
    
    private Date convertToDate(Object value) {
        if(value == null) return null;

        // en caso de ser java.sql.Date
        if(value instanceof Date) {
            return (Date) value;
        }

        // en caso de ser java.util.Date
        if(value instanceof java.util.Date){
            return new Date(((java.util.Date) value).getTime());
        }

        // si la fecha es un String
        if(value instanceof String){
            return parseDate((String) value);
        }

        return null;
    }

    private Date parseDate(String dateStr) {
        if(dateStr == null || dateStr.trim().isEmpty()){
            return null;
        }

        // realizamos una lista de fechas permitidas
        String[] dateFormaters = {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "dd.MM.yyyy",
        };

        for(String format: dateFormaters){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(dateStr);
                return new Date(parsed.getTime());                
            } catch (Exception e) {
                // continua con el siguiente formato
            }
        }

        log.error("Invalid date format: {}", dateStr);
        throw new IllegalArgumentException("Invalid date format: "+ dateStr + ". Expected formats: yyyy-MM-dd, dd/MM/yyyy, etc.");
    }
}
