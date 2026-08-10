package com.safiraenergia.mercadospot.etl.transformer;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.etl.transformer.validation.ValidationChain;
import com.safiraenergia.mercadospot.exceptions.TransformationException;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.utils.ETLLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataTransformer {

    @Autowired
    private ValidationChain validationChain;

    @Autowired
    private ETLLogger etlLogger;

    private static final Pattern EXCEL_FORMULA_PATTERN = Pattern.compile("^_xlfn\\.|^=.*$|^\\[.*\\].*$");
    private static final Pattern RUT_PATTERN = Pattern.compile("^\\d{1,8}-[\\dkK]$");
    private static final Set<String> INVALID_DATE_VALUES = Set.of(
            "S/I", "N/A", "SIN INFORMACION", "SIN INFO",
            "NO APLICA", "NA", "---", "___", "NULL",
            "VACIO", "EMPTY", "S/F");

    private static final Map<String, String> CODIGO_ESTADO_MAP = new HashMap<>();

    public List<FacturaDTO> transform(List<Map<String, Object>> rowData, String jobId) throws TransformationException {
        List<FacturaDTO> facturas = new ArrayList<>();
        int errorCount = 0;
        int skipCount = 0;

        if (rowData == null || rowData.isEmpty()) {
            log.warn("No data to transform");
            return facturas;
        }

        // Log de columnas disponibles
        if (!rowData.isEmpty()) {
            Map<String, Object> firstRow = rowData.get(0);
            log.info("Columnas disponibles en el Excel: {}", firstRow.keySet());
            log.info("Contenido de la primera fila: {}", firstRow);
        }

        for (int i = 0; i < rowData.size(); i++) {
            Map<String, Object> row = rowData.get(i);
            try {
                String tipoEntidad = (String) row.get("_tipoEntidad");
                if(tipoEntidad == null || tipoEntidad.trim().isEmpty()) {
                    tipoEntidad = "DEUDOR";
                }
                log.debug("Procesando fila {} - Tipo: {}", i, tipoEntidad);

                Long folio = getLongValueFromRow(row, "Folio", "N°", "Folio Factura", "N° Folio Factura");
                if (folio == null || folio <= 0) {
                    log.warn("Fila {} tiene folio nulo - Saltando", i);
                    skipCount++;
                    continue;
                }

                // Obtenemos la fecha de emision (con sus diferentes nombres)
                java.util.Date fechaEmisionUtil = getDateValueFromRow(row, "Fecha de Emision", "Fecha a Emision", "Fecha de Emisión", "Fecha Emision", "Fecha", " Fecha ", "Fecha ");
                if (fechaEmisionUtil == null) {
                    log.warn("Fila {} tiene fecha de emisión nula - Saltando", i);
                    skipCount++;
                    continue;
                }

                Double montoNeto = getDoubleValueFromRow(row," Neto ", "Neto", "Monto Neto", "monto_neto");
                Double montoBruto = getDoubleValueFromRow(row," Bruto ", "Bruto", "Monto Bruto", "monto_bruto");
                Double montoTotal = getDoubleValueFromRow(row," Total ", "Total", "Monto Total", "monto_total");
                
                String rutEntidad = getStringValueFromRow(row,"RUT Acreedor", "RUT Deudor", "RUT Entidad", "Rut", "RUT", "rut_entidad");
                String nomEntidad = getStringValueFromRow(row,"Nemotecnico Acreedor", "Nemotecnico Deudor", "Nombre Entidad", "Nombre", "Entidad", "nomentidad");
                String glosa = getStringValueFromRow(row,"GLOSA", "Glosa", "glosa");
                String estado = getStringValueFromRow(row,"Estado de Pago", "Estado", "estado");

                // log de las filas encontradas
                if(i < 5) {
                    log.info("=== FILA {} ===", i);
                    log.info("  Folio: {}", folio);
                    log.info("  Monto Neto: {}", montoNeto);
                    log.info("  RUT: {}", rutEntidad);
                    log.info("  Nombre: {}", nomEntidad);
                    log.info("  Glosa: {}", glosa);
                    log.info("  Estado: {}", estado);
                    log.info("  Fecha Emision: {}", fechaEmisionUtil);
                }

                // 🔥 Crear DTO usando builder
                FacturaDTO.FacturaDTOBuilder builder = FacturaDTO.builder()
                    .folio(folio)
                    .montoNeto(montoNeto != null ? montoNeto : 0.0)
                    .montoBruto(montoBruto != null ? montoBruto : 0.0)
                    .montoTotal(montoTotal != null ? montoTotal : 0.0)
                    .rutEntidad(rutEntidad)
                    .nomEntidad(nomEntidad)
                    .glosa(glosa)
                    .estado(estado)
                    .tipoEntidad(tipoEntidad);

                // 🔥 Procesar fechas (convertir a java.sql.Date)
                Date fechaEmision = new java.sql.Date(fechaEmisionUtil.getTime());
                builder.fechaEmision(fechaEmision);

                // fecha de pago
                java.util.Date fechaPagoUtil = getDateValueFromRow(row, "Fecha de Pago", "Fecha Pago", "Fecha", "Fecha ", " Fecha ");
                Date fechaPago = null;
                if (fechaPagoUtil != null) {
                    fechaPago = new java.sql.Date(fechaPagoUtil.getTime());
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fechaEmision);
                    cal.add(Calendar.DAY_OF_MONTH, 30);
                    fechaPago = new java.sql.Date(cal.getTimeInMillis());
                    log.debug("Fecha de pago derivada para folio {}", folio);
                }
                builder.fechaPago(fechaPago);

                if (fechaEmisionUtil != null) {
                    fechaEmision = new java.sql.Date(fechaEmisionUtil.getTime());
                }

                // 🔥 Validar fechas
                if (fechaEmision != null) {
                    Periodo periodo = Periodo.builder()
                            .mes(fechaEmision)
                            .build();
                    builder.periodo(fechaEmision);
                }

                // 🔥 Construir y validar DTO
                FacturaDTO dto = builder.build();

                // Validamos el DTO con logs detallados
                if (validateDtoWithLogs(dto, i)) {
                    facturas.add(dto);
                } else {
                    log.warn("Fila {} - DTO inválido", i);
                    skipCount++;
                }

            } catch (Exception e) {
                errorCount++;
                if (errorCount <= 10) {
                    etlLogger.logError(jobId, "Error transforming row " + i, e);
                }
            }
        }

        etlLogger.logInfo(jobId, "Transformation completed - Success: " + facturas.size() +
                ", Errors: " + errorCount + ", Skipped: " + skipCount);
        return facturas;
    }

    // metodo de la validacion con logs
    private boolean validateDtoWithLogs(FacturaDTO dto, int rowIndex) {
        if (dto == null) {
            log.warn("Fila {} - DTO es null", rowIndex);
            return false;
        }
        
        if (dto.getFolio() == null || dto.getFolio() <= 0) {
            log.warn("Fila {} - Folio inválido: {}", rowIndex, dto.getFolio());
            return false;
        }
        
        if (dto.getPeriodo() == null) {
            log.warn("Fila {} - Periodo null para folio: {}", rowIndex, dto.getFolio());
            return false;
        }
        
        if (dto.getGlosa() == null || dto.getGlosa().trim().isEmpty()) {
            log.warn("Fila {} - Glosa vacía para folio: {}", rowIndex, dto.getFolio());
            return false;
        }
        
        if (dto.getRutEntidad() == null || dto.getRutEntidad().trim().isEmpty()) {
            log.warn("Fila {} - RUT vacío para folio: {}", rowIndex, dto.getFolio());
            return false;
        }
        
        if (dto.getNomEntidad() == null || dto.getNomEntidad().trim().isEmpty()) {
            log.warn("Fila {} - Nombre entidad vacío para folio: {}", rowIndex, dto.getFolio());
            return false;
        }
        
        if (dto.getMontoNeto() == null || dto.getMontoNeto() <= 0) {
            log.warn("Fila {} - montoNeto inválido para folio: {} (valor: {})", 
                rowIndex, dto.getFolio(), dto.getMontoNeto());
            return false;
        }

        return true;
    }

    // 🔥 Métodos auxiliares
    private Long getLongValue(Object value) {
        if (value == null)
            return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            String str = value.toString().trim();
            if (str.isEmpty())
                return null;
            return Long.parseLong(str);
        } catch (Exception e) {
            log.error("Error con getLongValue: {}", e.getMessage());
            return null;
        }
    }

    // 🔥 Retorna Double en lugar de double
    private Double getDoubleValue(Object value) {
        if (value == null)
            return 0.0;
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            String str = value.toString().trim().replace("$", "").replace(",", "");
            if (str.isEmpty())
                return 0.0;
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Metodos auxliares diferentes
    private Long getLongValueFromRow(Map<String, Object> row, String... possibleKeys) {
        for (String key : possibleKeys) {
            Object value = row.get(key);
            if (value != null) {
                try {
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    String str = value.toString().trim();
                    if (!str.isEmpty()) {
                        return Long.parseLong(str.replaceAll("[^\\d]", ""));
                    }
                } catch (Exception e) {
                    log.debug("No se pudo convertir '{}' a Long: {}", key, value);
                }
            }
        }
        return null;
    }

    private Double getDoubleValueFromRow(Map<String, Object> row, String... possibleKeys) {
        for (String key : possibleKeys) {
            Object value = row.get(key);
            if (value != null) {
                try {
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    String str = value.toString().trim()
                        .replace("$", "")
                        .replace(",", "")
                        .replace(" ", "");
                    if (!str.isEmpty()) {
                        try {
                            return Double.parseDouble(str);
                        } catch (Exception e) {
                            log.warn("No se pudo convertir '{}' a Double: {}", key, value);
                        }
                    }
                } catch (Exception e) {
                    log.debug("No se pudo convertir '{}' a Double: {}", key, value);
                }
            }
        }
        return null;
    }

    private String getStringValueFromRow(Map<String, Object> row, String... possibleKeys) {
        for (String key : possibleKeys) {
            Object value = row.get(key);
            if (value != null) {
                String str = value.toString().trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }

        log.debug("No se encontró valor para keys: {}", (Object) possibleKeys);
        return null;
    }

    private java.util.Date getDateValueFromRow(Map<String, Object> row, String... possibleKeys) {
        for (String key : possibleKeys) {
            Object value = row.get(key);
            if (value != null) {
                try {
                    if (value instanceof java.util.Date) {
                        return (java.util.Date) value;
                    }
                    if (value instanceof java.sql.Date) {
                        return new java.util.Date(((java.sql.Date) value).getTime());
                    }
                    if (value instanceof Number) {
                        double excelData = ((Number) value).doubleValue();
                        java.util.Date utilDate = DateUtil.getJavaDate(excelData);
                        if (utilDate != null) {
                            return utilDate;
                        }
                    }
                    String str = value.toString().trim();
                    if (!str.isEmpty()) {
                        String upperStr = str.toUpperCase();
                        if (INVALID_DATE_VALUES.contains(upperStr)) {
                            return null;
                        }
                        return parseDate(str);
                    }
                } catch (Exception e) {
                    log.debug("No se pudo convertir '{}' a Date: {}", key, value);
                }
            }
        }
        return null;
    }

    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        String upperDateStr = dateStr.toUpperCase();
        if (INVALID_DATE_VALUES.contains(upperDateStr)) {
            return null;
        }

        if (EXCEL_FORMULA_PATTERN.matcher(dateStr).find()) {
            return null;
        }

        if (dateStr.matches("^\\d+(\\.\\d+)?$")) {
            try {
                double excelDate = Double.parseDouble(dateStr);
                java.util.Date utilDate = DateUtil.getJavaDate(excelDate);
                if (utilDate != null) {
                    return utilDate;
                }
            } catch (NumberFormatException e) {
                log.error("Number no valid: {}", e);
            }
        }

        String[] dateFormats = {
                "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy/MM/dd", "dd.MM.yyyy",
                "EEE MMM dd HH:mm:ss zzz yyyy", "EEE MMM dd yyyy", "MM/dd/yyyy",
                "dd-MMM-yyyy", "yyyyMMdd", "ddMMyyyy"
        };

        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date parsed = sdf.parse(dateStr);
                return parsed;
            } catch (Exception e) {
                // continuar
            }
        }

        log.debug("Could not parse date: {}", dateStr);
        return null;
    }

    private String getStringValue(Object value) {
        if (value == null)
            return null;
        return value.toString().trim();
    }

    // 🔥 Retorna java.util.Date en lugar de java.sql.Date
    private java.util.Date getDateValue(Object value) {
        if (value == null)
            return null;
        try {
            if (value instanceof java.util.Date) {
                return (java.util.Date) value;
            }
            if (value instanceof java.sql.Date) {
                return new java.util.Date(((java.sql.Date) value).getTime());
            }
            if (value instanceof Number) {
                double excelData = ((Number) value).doubleValue();
                java.util.Date utilDate = DateUtil.getJavaDate(excelData);
                if (utilDate != null) {
                    return utilDate;
                }
                return null;
            }
            String str = value.toString().trim();
            if (str.isEmpty())
                return null;

            // Verificar valores especiales
            String upperStr = str.toUpperCase();
            if (INVALID_DATE_VALUES.contains(upperStr)) {
                return null;
            }

            return parseDate(str);
        } catch (Exception e) {
            log.warn("Error parseando fecha: {}", value);
            return null;
        }
    }

    private boolean validateDto(FacturaDTO dto) {
        if (dto == null) return false;
        if (dto.getFolio() == null || dto.getFolio() <= 0) return false;
        if (dto.getPeriodo() == null) return false;
        if (dto.getGlosa() == null || dto.getGlosa().trim().isEmpty()) return false;
        if (dto.getRutEntidad() == null || dto.getRutEntidad().trim().isEmpty()) return false;
        if (dto.getNomEntidad() == null || dto.getNomEntidad().trim().isEmpty()) return false;

        if (dto.getMontoNeto() == null) {
            log.debug("Invalid montoNeto: {}", dto.getMontoNeto());
            return false;
        }
        return true;
    }
}