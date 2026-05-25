package com.safiraenergia.mercadospot.etl.extractor.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
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
public class ExcelDataExtractor implements DataExtractor{
    
    @Override
    public List<Map<String, Object>> extract(InputStream source) throws ExtractionException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(source)){
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            List<String> hearders = getHeaders(headerRow);

            for(int i = 1; i <= sheet.getLastRowNum(); i++){
                Row row = sheet.getRow(i);
                if(row != null && !isRowEmpty(row)){
                    Map<String, Object> rowData = new HashMap<>();
                    for (int j = 0; j < hearders.size(); j++) {
                        Cell cell = row.getCell(j);
                        rowData.put(hearders.get(j), getCellValue(cell));
                    }
                    data.add(rowData);
                }
            }
            log.info("Extracted {} records from Excel file", data.size());
        } catch (Exception e) {
            log.error("Error extracting data from Excel", e);
        }

        return data;
    }

    @Override
    public boolean supports(String fileType) {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(fileType) || fileType.endsWith(".xlsx");
    }

    private List<String> getHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String headerValue = getCellValue(cell).toString().trim();
            headers.add(headerValue);
        }
        log.info("Headers encontrados_ {}", headers);
        return headers;
    }

    // método para detectar fórmulas
    private boolean isFormulaCell(Cell cell){
        return cell != null && cell.getCellType() == CellType.FORMULA;
    }

    private Object getCellValue(Cell cell){
        if(cell == null) return null;

        // si es fórmula, intentar obtener el valor cached o retornar null
        if(cell.getCellType() == CellType.FORMULA){
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
                if(DateUtil.isCellDateFormatted(cell)){
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
            if(cell != null && cell.getCellType() != CellType.BLANK){
                return false;
            }
        }
        return true;
    }
}
