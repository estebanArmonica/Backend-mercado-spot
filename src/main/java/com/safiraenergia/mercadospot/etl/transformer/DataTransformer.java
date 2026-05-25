package com.safiraenergia.mercadospot.etl.transformer;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.etl.transformer.validation.ValidationChain;
import com.safiraenergia.mercadospot.exceptions.TransformationException;
import com.safiraenergia.mercadospot.utils.ETLLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataTransformer {
    
    @Autowired
    private ValidationChain validationChain;

    @Autowired
    private ETLLogger etlLogger;

    // constante para detectar fórmulas de Excel
    private static final Pattern EXCEL_FORMULA_PATTERN = Pattern.compile("^_xlfn\\.|^=.*$|^\\[.*\\].*$");
    private static final Pattern RUT_PATTERN = Pattern.compile("^\\d{1,8}-[\\dkK]$");

    // mapeo de código de estado a estados reales
    private static final Map<String, String> CODIGO_ESTADO_MAP = new HashMap<>();

    public List<FacturaDTO> transform(List<Map<String, Object>> rowData, String jobId) throws TransformationException {
        List<FacturaDTO> facturas = new ArrayList<>();
        int errorCount = 0;
        int skipCount = 0;

        if(rowData == null || rowData.isEmpty()){
            log.warn("No data to transform");
            return facturas;
        }

        // log de columnas disponibles
        if(!rowData.isEmpty()) {
            log.info("Columnas disponibles en el Excel: {}", rowData.get(0).keySet());
        }

        for (int i = 0; i < rowData.size(); i++) {
            Map<String, Object> row = rowData.get(i);
            try {
                FacturaDTO dto = transformRow(row, i, jobId);

                // validamos que el DTO tenga datos mínimos antes de agregar
                if(dto.getFolio() != null && dto.getFolio() > 0){
                    validationChain.validate(dto);
                    facturas.add(dto);
                } else {
                    if (i < 10) {
                        etlLogger.logWarning(jobId, "Row " + i + " has invalid folio: " + dto.getFolio());
                    }
                    skipCount++;
                }
            } catch (Exception e) {
                errorCount++;
                if(errorCount <= 10){
                    etlLogger.logError(jobId, "Error transforming row " + i, e);
                }
            }
        }

        etlLogger.logInfo(jobId, "Transformation completed - Success: " + facturas.size() + 
                         ", Errors: " + errorCount + ", Skipped: " + skipCount);
        return facturas;
    }

    private FacturaDTO transformRow(Map<String, Object> row, int rowIndex, String jobId) {
        // Obtener valores originales
        String estadoOriginal = convertToString(getValueFromRow(row, "Estado de Pago", "Estado"));
        String fechaEmisionOriginal = convertToString(getValueFromRow(row, "fecha_emision", "Fecha de Emision", "Fecha Emision", "Fecha Emisión"));
        String fechaPagoOriginal = convertToString(getValueFromRow(row, "fecha_pago", "Fecha de Pago", "Fecha Pago"));
        
        // Limpiar fórmulas de Excel
        String estadoLimpio = limpiarFormulaExcel(estadoOriginal);
        String fechaEmisionLimpia = limpiarFormulaExcel(fechaEmisionOriginal);
        String fechaPagoLimpia = limpiarFormulaExcel(fechaPagoOriginal);
        
        // Normalizar estado
        String estadoNormalizado = normalizarEstado(estadoLimpio);
        
        // Procesar fechas
        Date fechaEmision = convertToDate(fechaEmisionLimpia);
        Date fechaPago = convertToDate(fechaPagoLimpia);
        
        // Si es PAGADA pero no tiene fecha de pago, derivarla
        if ("PAGADA".equals(estadoNormalizado) && fechaPago == null) {
            if (fechaEmision != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaEmision);
                cal.add(Calendar.DAY_OF_MONTH, 30);
                fechaPago = new Date(cal.getTimeInMillis());
                etlLogger.logInfo(jobId, "Derived fechaPago from fechaEmision for folio: " + row.get("folio"));
            } else {
                fechaPago = new Date(System.currentTimeMillis());
            }
        }
        
        return FacturaDTO.builder()
            .folio(convertToLong(getValueFromRow(row, "folio", "Folio", "N° Folio Factura")))
            .montoNeto(convertToInteger(getValueFromRow(row, "monto_neto", "Neto")))
            .montoBruto(convertToInteger(getValueFromRow(row, "monto_bruto", "Bruto")))
            .montoTotal(convertToInteger(getValueFromRow(row, "monto_total", "Total", "Monto Total")))
            .fechaEmision(fechaEmision)
            .fechaPago(fechaPago)
            .rutEntidad(convertToString(getValueFromRow(row, "rut_entidad", "RUT Acreedor", "RUT Deudor", "Rut", "RUT")))
            .nomEntidad(convertToString(getValueFromRow(row, "nomentidad", "Nemotecnico Acreedor", "Nemotecnico Deudor", "Nombre", "Entidad")))
            .glosa(convertToString(getValueFromRow(row, "glosa", "GLOSA", "Glosa")))
            .periodo(convertToDate(getValueFromRow(row, "periodo", "Período", "Periodo", "Fecha")))
            .estado(estadoNormalizado)
            .estadoOriginal(estadoLimpio)  // Guardar el valor original
            .tipoEntidad(detectarTipoEntidad(row, rowIndex))
            .build();
    }

    /**
     * Limpia fórmulas de Excel y devuelve el valor real
    */
    private String limpiarFormulaExcel(String value) {
        if (value == null) return null;
        
        String cleanValue = value.trim();
        
        // Si es una fórmula de Excel, intentar extraer el valor real
        if (EXCEL_FORMULA_PATTERN.matcher(cleanValue).find()) {
            // Para fórmulas de BUSCARX, no podemos evaluar, retornar null
            if (cleanValue.contains("BUSCARX") || cleanValue.contains("XLOOKUP")) {
                return null;
            }
            // Para concatenaciones como RUT&Neto, intentar extraer solo el RUT
            if (cleanValue.contains("&")) {
                // Intentar extraer el RUT de la fórmula
                String[] parts = cleanValue.split("&");
                if (parts.length > 0) {
                    String posibleRut = parts[0].replaceAll("[^\\d-]", "");
                    if (RUT_PATTERN.matcher(posibleRut).matches()) {
                        return posibleRut;
                    }
                }
            }
            return null;
        }
        
        return cleanValue;
    }

    /**
     * Detecta si la fila corresponde a un DEUDOR o ACREDOOR basado en las columnas presentes
    */
    private String detectarTipoEntidad(Map<String, Object> row, int rowIndex) {
        // si tiene "RUT Acreedor o Nemotecnico Acreedor" es ACREEDOR
        if (row.containsKey("RUT Acreedor") || row.containsKey("Nemotecnico Acreedor")) {
            return "Acreedor";
        }

        // Si tiene "RUT Deudor" o "Nemotecnico Deudor" es DEUDOR
        if (row.containsKey("RUT Deudor") || row.containsKey("Nemotecnico Deudor")) {
            return "Deudor";
        }

        // por defecto retornara a DEUDOR
        return "Deudor";
    }


    /**
     * Normaliza el estado de pago a los valores esperados por la base de datos
     * Maneja:
     * - Palabras: Pagado, Pendiente, Vencido, Anulado, Parcial
     * - Códigos numéricos: si el código tiene más de 10 caracteres, se considera como identificador único
     * - Valores vacíos: por defecto PENDIENTE
    */
    private String normalizarEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return "PENDIENTE";
        }
        
        String estadoLower = estado.toLowerCase().trim();
        
        // Palabras clave
        if (estadoLower.contains("pagado") || estadoLower.contains("pagada") || estadoLower.contains("paid")) {
            return "PAGADA";
        }
        if (estadoLower.contains("vencido") || estadoLower.contains("vencida") || estadoLower.contains("expired")) {
            return "VENCIDA";
        }
        if (estadoLower.contains("anulado") || estadoLower.contains("anulada") || estadoLower.contains("cancelled")) {
            return "ANULADA";
        }
        if (estadoLower.contains("parcial")) {
            return "PARCIAL";
        }
        if (estadoLower.contains("pendiente") || estadoLower.contains("pending")) {
            return "PENDIENTE";
        }
        
        // Si es un código como "76519771-6295" (RUT-xxxx)
        if (estado.matches("^\\d{7,8}-\\d+$")) {
            // Por defecto, considerarlo como pagado si es un código de acreedor
            return "PAGADA";
        }
        
        return "PENDIENTE";
    }

    /**
     * Detecta el tipo de estado basado en el código numérico
    */
    private String detectarTipoEstadoPorCodigo(String codigo) {
        // Si el código contiene ciertos patrones, se puede determinar el estado
        if (codigo.contains("0") && !codigo.contains("1")) {
            return "PENDIENTE";
        }
        if (codigo.contains("1") && codigo.length() > 10) {
            return "PAGADA";
        }
        // Por defecto, pendiente
        return "PENDIENTE";
    }

    /**
     * Obtiene el valor de la fila buscando por múltiples nombres de columnas posibles
    */
    private Object getValueFromRow(Map<String, Object> row, String... possibleKeys) {
        if (row == null) return null;
        
        for (String key : possibleKeys) {
            if (row.containsKey(key)) {
                Object value = row.get(key);
                if (value != null && !value.toString().isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Limpiamos las fórmulas de Excel y devuelve el valor real si es posible
    */
    private Object cleanExcelFormula(Object value){
        if(value == null) return null;

        String strValue = value.toString();

        // si es una fórmula de Excel, intenta extraer algún valor o retorna null
        if(EXCEL_FORMULA_PATTERN.matcher(strValue).find()){
            log.debug("Detected Excel formula: {}", strValue);
            return null;
        }

        return value;
   }

    private Long convertToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            String str = ((String) value).trim().replaceAll("[^\\d]", "");
            if (str.isEmpty()) return null;
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Integer convertToInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String str = ((String) value).trim().replaceAll("[^\\d]", "");
            if (str.isEmpty()) return 0;
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private String convertToString(Object value) {
        if(value == null) return null;
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }
    
    private Date convertToDate(Object value) {
        if(value == null) return null;
        if(value instanceof Date) return (Date) value;
        if(value instanceof java.util.Date) return new Date(((java.util.Date) value).getTime());
        if(value instanceof String) return parseDate((String) value);
        return null;
    }

    private Date parseDate(String dateStr) {
        if(dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();
        
        String[] dateFormats = {
            "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "dd.MM.yyyy",
            "EEE MMM dd HH:mm:ss zzz yyyy", "EEE MMM dd yyyy", "MM/dd/yyyy"
        };

        for(String format: dateFormats){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(dateStr);
                return new Date(parsed.getTime());                
            } catch (Exception e) {
                // continuar
            }
        }
        return null;
    }
}
