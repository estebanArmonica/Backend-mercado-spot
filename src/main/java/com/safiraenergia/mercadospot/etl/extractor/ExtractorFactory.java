package com.safiraenergia.mercadospot.etl.extractor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ExtractorFactory {
    private final Map<String, DataExtractor> extractores = new ConcurrentHashMap<>();
    private final List<DataExtractor> extractorList;
    
    @Autowired
    public ExtractorFactory(List<DataExtractor> extractorList){
        this.extractorList = extractorList;
    }

    @PostConstruct
    public void init(){
        for (DataExtractor extractor: extractorList) {
            extractores.put(extractor.getClass().getSimpleName(), extractor);
        }
    }

    public DataExtractor getExtractor(String fileType) {
        return extractorList.stream()
                .filter(e -> e.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Extractor found for file type: " + fileType));
    }
}
