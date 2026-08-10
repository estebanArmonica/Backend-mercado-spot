package com.safiraenergia.mercadospot.etl.extractor.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.safiraenergia.mercadospot.etl.extractor.DataExtractor;
import com.safiraenergia.mercadospot.exceptions.ExtractionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExcelDataExtractor implements DataExtractor {

    private static final String SHEET_DEUDOR = "Cargos (Deudor)";
    private static final String SHEET_ACREEDOR = "Abonos (Acreedor)";

    @Override
    public List<Map<String, Object>> extract(InputStream source) throws ExtractionException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(source)) {
            int numberOfSheets = workbook.getNumberOfSheets();
            log.info("El archivo tiene {} hojas", numberOfSheets);

            // listamos todas las hojas disponibles
            for (int i = 0; i < numberOfSheets; i++) {
                String sheetName = workbook.getSheetName(i);
                log.info("Hoja {}: '{}'", i, sheetName);
            }

            // procesamos las hojas de Deudor
            Sheet sheetDeudor = workbook.getSheet(SHEET_DEUDOR);
            if (sheetDeudor != null) {
                log.info("Procesando hoja de DEUDOR: '{}'", SHEET_DEUDOR);
                List<Map<String, Object>> deudorData = processSheet(sheetDeudor, "DEUDOR");

                data.addAll(deudorData);
                log.info("Extraídos {} registros de la hoja DEUDOR", deudorData.size());
            } else {
                log.warn("No se encontró la hoja '{}'", SHEET_DEUDOR);

                // Fallback: usamos la primera hoja
                Sheet firstSheet = workbook.getSheetAt(0);
                log.info("Usando primera hoja como alternativa: '{}'", workbook.getSheetName(0));
                List<Map<String, Object>> firstSheetData = processSheet(firstSheet, "DEUDOR");
                data.addAll(firstSheetData);
            }

            // Procesar hoja de Acreedor
            Sheet sheetAcreedor = workbook.getSheet(SHEET_ACREEDOR);
            if (sheetAcreedor != null) {
                log.info("Procesando hoja de ACREEDOR: '{}'", SHEET_ACREEDOR);

                log.info("Hoja ACREEDOR - Última fila: {}", sheetAcreedor.getLastRowNum());
                Row headerRow = sheetAcreedor.getRow(0);
                if (headerRow != null) {
                    log.info("Hoja ACREEDOR - Headers: {}", getHeaders(headerRow));
                }

                List<Map<String, Object>> acreedorData = processSheet(sheetAcreedor, "ACREEDOR");

                log.info("Extraídos {} registros de la hoja ACREEDOR", acreedorData.size());
                data.addAll(acreedorData);
            } else {

                log.warn("No se encontró la hoja '{}'", SHEET_ACREEDOR);
            }

            log.info("Total de registros extraídos: {} de {} hojas procesadas", data.size(),
                    (sheetDeudor != null ? 1 : 0) + (sheetAcreedor != null ? 1 : 0));

        } catch (Exception e) {
            log.error("Error extracting Excel data", e);
            throw new ExtractionException("Failed to extract data from Excel", e);
        }

        return data;
    }

    /**
     * Procesamos la hoja especifica de Excel
     * 
     * @param sheet:       Hoja a procesar
     * @param tipoEntidad: tipo de la entidad (DEUDOR, ACREEDOR)
     * @return: Retornamos una lista con los datos de la hoja
    */
    private List<Map<String, Object>> processSheet(Sheet sheet, String tipoEntidad) {
        List<Map<String, Object>> data = new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            log.warn("Hoja '{}' no tiene fila de encabezados", tipoEntidad);
            return data;
        }

        // Obtener headers
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            headers.add(getCellValueAsString(cell).trim());
        }

        log.info("Headers encontrados en hoja {}: {}", tipoEntidad, headers);

        // Evaluador de fórmulas
        FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();

        // Procesar filas de datos
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            Map<String, Object> rowData = new HashMap<>();
            boolean hasData = false;

            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                Object value = getCellValueSafely(cell, evaluator);

                if (value != null && !value.toString().isEmpty()) {
                    hasData = true;
                }
                rowData.put(headers.get(j), value);
            }

            // Agregar tipo de entidad para identificar el origen
            rowData.put("_tipoEntidad", tipoEntidad);

            if (hasData) {
                data.add(rowData);
            }
        }

        return data;
    }

    @Override
    public boolean supports(String fileType) {
        // Aceptar tipos de archivo de Excel
        return fileType != null
                && (fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || // .xlsx
                        fileType.equals("application/vnd.ms-excel") || // .xls
                        fileType.equals("application/octet-stream") || // algunos sistemas usan esto
                        fileType.endsWith(".xlsx") ||
                        fileType.endsWith(".xls"));
    }

    /**
     * Obtiene el valor de una celda de forma segura, manejando referencias externas
     */
    private Object getCellValueSafely(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null)
            return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        log.debug("Fecha encontrada (NUMERIC): {}", date);
                        return date;
                    }
                    return cell.getNumericCellValue();

                case BOOLEAN:
                    return cell.getBooleanCellValue();

                case FORMULA:
                    // intentamos obtener el valor de cached
                    try {
                        if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                log.debug("Fecha encontrada (FORMULA cached): {}", date);

                                return date;
                            }
                            return cell.getNumericCellValue();
                        }
                        if (cell.getCachedFormulaResultType() == CellType.STRING) {
                            return cell.getStringCellValue();
                        }
                    } catch (Exception e) {
                        log.debug("No cached value for formula");
                    }

                    // Intentar evaluar la fórmula
                    try {
                        CellValue cellValue = evaluator.evaluate(cell);
                        switch (cellValue.getCellType()) {
                            case STRING:
                                return cellValue.getStringValue();
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    return cell.getDateCellValue();
                                }
                                return cellValue.getNumberValue();
                            case BOOLEAN:
                                return cellValue.getBooleanValue();
                            default:
                                log.debug("Formula evaluated to unknown type: {}", cell.getCellFormula());
                                return null;
                        }
                    } catch (Exception e) {
                        // Si hay error evaluando la fórmula (como referencia externa), retornar null
                        log.debug("Could not evaluate formula: {} - {}", cell.getCellFormula(), e.getMessage());
                    }
                    return null;

                default:
                    return null;
            }
        } catch (Exception e) {
            log.debug("Error reading cell: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el valor de una celda, evaluando fórmulas de Excel si es necesario
     */
    private Object getCellValueWithFormula(Cell cell, FormulaEvaluator evaluator) {

        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    // evaluamos las formulas de excel
                    CellValue cellValue = evaluator.evaluate(cell);
                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue();
                        case NUMERIC:
                            return cellValue.getNumberValue();
                        case BOOLEAN:
                            return cellValue.getBooleanValue();
                        default:
                            return cell.getCellFormula();
                    }
                } catch (Exception e) {
                    log.warn("Error evaluating formula: {}", cell.getCellFormula(), e);
                    return cell.getCellFormula();
                }
            default:
                return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // Metodo Aux
    private List<String> getHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        if (headerRow == null)
            return headers;

        for (Cell cell : headerRow) {
            String value = getCellValueAsString(cell).trim();
            headers.add(value);
        }
        log.info("Headers encontrados: {}", headers);
        return headers;
    }

    // método para detectar fórmulas
    private boolean isFormulaCell(Cell cell) {
        return cell != null && cell.getCellType() == CellType.FORMULA;
    }

    private Object getCellValue(Cell cell) {
        if (cell == null)
            return null;

        // si es fórmula, intentar obtener el valor cached o retornar null
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        return cell.getNumericCellValue();
                    case STRING:
                        return cell.getStringCellValue();
                    default:
                        return cell.getCellFormula();
                }
            } catch (Exception e) {
                log.warn("Cannot evaluate formula: {}", cell.getCellFormula());
                return cell.getCellFormula();
            }
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
