package com.safiraenergia.mercadospot.etl.extractor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.safiraenergia.mercadospot.exceptions.ExtractionException;

// utilizamos el patron de diseño strategy
public interface DataExtractor {
    List<Map<String, Object>> extract(InputStream source) throws ExtractionException;
    boolean supports(String fileType);
}
