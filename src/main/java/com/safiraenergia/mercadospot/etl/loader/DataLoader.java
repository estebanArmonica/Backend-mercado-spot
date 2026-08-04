package com.safiraenergia.mercadospot.etl.loader;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.etl.loader.service.ETLTransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataLoader {

    @Autowired
    private ETLTransactionService transactionService;

    public LoadResult load(List<FacturaDTO> facturas) {
        if (facturas == null || facturas.isEmpty()) {
            log.warn("No hay facturas para cargar");
            return new LoadResult(0, 0, 0, new ArrayList<>());
        }
        
        int totalInserted = 0;
        int totalSkipped = 0;
        List<String> allErrors = new ArrayList<>();
        
        // 🔥 Batch size ajustado para evitar timeout
        int batchSize = 50; // 🔥 Aumentado de 25 a 50
        int totalBatches = (facturas.size() + batchSize - 1) / batchSize;
        
        log.info("Iniciando carga de {} facturas en {} batches de {} registros", 
            facturas.size(), totalBatches, batchSize);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < facturas.size(); i += batchSize) {
            int end = Math.min(i + batchSize, facturas.size());
            List<FacturaDTO> batch = facturas.subList(i, end);
            int batchNum = (i / batchSize) + 1;
            
            try {
                log.debug("Procesando batch {} de {} (registros {}-{})", 
                    batchNum, totalBatches, i + 1, end);
                
                // 🔥 Procesar batch en su propia transacción
                LoadResult batchResult = transactionService.processBatch(batch);
                
                totalInserted += batchResult.getInserted();
                totalSkipped += batchResult.getSkipped();
                allErrors.addAll(batchResult.getErrors());
                
                log.debug("Batch {} completado - Insertados: {}, Saltados: {}, Errores: {}", 
                    batchNum, batchResult.getInserted(), batchResult.getSkipped(), 
                    batchResult.getErrors().size());
                
            } catch (Exception e) {
                log.error("Error procesando batch {} (registros {}-{})", batchNum, i + 1, end, e);
                allErrors.add("Batch " + batchNum + " (registros " + (i+1) + "-" + end + ") falló: " + e.getMessage());
                totalSkipped += batch.size();
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Carga completada en {} ms - Insertados: {}, Saltados: {}, Errores: {}", 
            duration, totalInserted, totalSkipped, allErrors.size());
        
        return new LoadResult(totalInserted, 0, totalSkipped, allErrors);
    }
}