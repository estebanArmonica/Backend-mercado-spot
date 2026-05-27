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
        int inserted = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        int batchSize = 25;
        
        for (int i = 0; i < facturas.size(); i += batchSize) {
            int end = Math.min(i + batchSize, facturas.size());
            List<FacturaDTO> batch = facturas.subList(i, end);
            
            try {
                // Cada batch se procesa en su propia transacción
                LoadResult batchResult = transactionService.processBatch(batch);

                inserted += batchResult.getInserted();
                skipped += batchResult.getSkipped();
                errors.addAll(batchResult.getErrors());

                log.debug("Batch processed - Inserted: {}, Skipped: {}", batchResult.getInserted(), batchResult.getSkipped());
            } catch (Exception e) {
                log.error("Error processing batch {}-{}", i, end, e);
                errors.add("Batch " + i +"-" + end +" failed: " + e.getMessage());
                
                skipped += batch.size();
            }
        }
        
        log.info("Load completed - Inserted: {}, Skipped: {}, Errors: {}", inserted, skipped, errors.size());
        
        return new LoadResult(inserted, 0, skipped, errors);
    }
}
