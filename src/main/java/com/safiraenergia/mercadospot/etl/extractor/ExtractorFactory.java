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
        System.out.println("=== ExtractorFactory Initialization ===");
        for (DataExtractor extractor : extractorList) {
            String className = extractor.getClass().getSimpleName();
            extractores.put(className, extractor);
            System.out.println("Registered extractor: " + className);
        }
        System.out.println("Total extractors registered: " + extractores.size());
    }

    public DataExtractor getExtractor(String fileType) {
        System.out.println("Looking for extractor for file type: " + fileType);
        
        return extractorList.stream()
            .filter(e -> {
                boolean supports = e.supports(fileType);
                System.out.println("  - " + e.getClass().getSimpleName() + " supports: " + supports);
                return supports;
            })
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No Extractor found for file type: " + fileType + 
                ". Available extractors: " + extractorList.size()
            ));
    }
}
