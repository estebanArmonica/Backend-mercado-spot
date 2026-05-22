package com.safiraenergia.mercadospot.services.etl.impl;

import com.safiraenergia.mercadospot.dto.etl.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.etl.ETLResultDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.enums.ETLStatus;
import com.safiraenergia.mercadospot.etl.extractor.DataExtractor;
import com.safiraenergia.mercadospot.etl.extractor.ExtractorFactory;
import com.safiraenergia.mercadospot.etl.loader.DataLoader;
import com.safiraenergia.mercadospot.etl.loader.LoadResult;
import com.safiraenergia.mercadospot.etl.transformer.DataTransformer;
import com.safiraenergia.mercadospot.exceptions.ETLException;
import com.safiraenergia.mercadospot.services.factura.IFacturaService;
import com.safiraenergia.mercadospot.services.etl.IETLProcessorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del servicio ETL
 * Aplicando Patrón Singleton, Patrón Observer y Principio de Responsabilidad Única (SRP)
*/
@Slf4j
@Service
public class ETLProcessorServiceImpl implements IETLProcessorService{
    
    private final ExtractorFactory extractorFactory;
    private final DataTransformer transformer;
    private final DataLoader loader;
    private final IFacturaService facturaService;

    @Value("${etl.batch.size:100}")
    private int batchSize;

    // Almacenamiento de trabajos en progreso
    private final Map<String, ETLProgressDTO> jobProgressMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ETLResultDTO>> jobFutureMap = new ConcurrentHashMap<>();

    @Autowired
    public ETLProcessorServiceImpl(ExtractorFactory extractorFactory, DataTransformer transformer, DataLoader loader, IFacturaService facturaService) {
        this.extractorFactory = extractorFactory;
        this.transformer = transformer;
        this.loader = loader;
        this.facturaService = facturaService;
    }
    
    @Override
    @Async("etlTaskExecutor")
    public CompletableFuture<ETLResultDTO> processExcelFile(MultipartFile file, String usuarioId) {
        String jobId = UUID.randomUUID().toString();
        ETLProgressDTO progress = initializeProgress(jobId, usuarioId, file.getOriginalFilename());
        
        try {
            // 1. Validar archivo
            validateFile(file);
            progress.setStatus(ETLStatus.VALIDATING);
            progress.setCurrentStep("Validando archivo...");
            
            // 2. Extraer datos
            progress.setStatus(ETLStatus.EXTRACTING);
            progress.setCurrentStep("Extrayendo datos del Excel...");
            DataExtractor extractor = extractorFactory.getExtractor(file.getContentType());
            List<Map<String, Object>> rawData = extractor.extract(file.getInputStream());
            progress.setRecordsExtracted(rawData.size());
            
            // 3. Transformar datos
            progress.setStatus(ETLStatus.TRANSFORMING);
            progress.setCurrentStep("Transformando datos...");
            List<FacturaDTO> facturas = transformer.transform(rawData);
            progress.setRecordsTransformed(facturas.size());
            
            // 4. Validar datos transformados
            progress.setStatus(ETLStatus.VALIDATING);
            progress.setCurrentStep("Validando datos...");
            validateFacturas(facturas);
            
            // 5. Cargar datos en batches
            progress.setStatus(ETLStatus.LOADING);
            LoadResult result = loadInBatches(facturas, progress);
            
            // 6. Completar proceso
            ETLResultDTO finalResult = buildResult(jobId, progress, result);
            progress.setStatus(ETLStatus.COMPLETED);
            progress.setProgress(100);
            
            log.info("ETL job {} completed successfully. Inserted: {}, Errors: {}", 
                     jobId, result.getInserted(), result.getErrors().size());
            
            return CompletableFuture.completedFuture(finalResult);
            
        } catch (Exception e) {
            log.error("ETL job {} failed", jobId, e);
            progress.setStatus(ETLStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            
            ETLResultDTO errorResult = ETLResultDTO.builder()
                .jobId(jobId)
                .status(ETLStatus.FAILED)
                .errorMessage(e.getMessage())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
            
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            jobProgressMap.put(jobId, progress);
        }
    }

    @Override
    public ETLProgressDTO getJobProgress(String jobId) {
        return jobProgressMap.getOrDefault(jobId, 
            ETLProgressDTO.builder()
                .jobId(jobId)
                .status(ETLStatus.NOT_FOUND)
                .build());
    }
    
    @Override
    public boolean cancelJob(String jobId) {
        CompletableFuture<ETLResultDTO> future = jobFutureMap.get(jobId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                ETLProgressDTO progress = jobProgressMap.get(jobId);
                if (progress != null) {
                    progress.setStatus(ETLStatus.CANCELLED);
                    progress.setErrorMessage("Job cancelled by user");
                }
                log.info("ETL job {} cancelled by user", jobId);
            }
            return cancelled;
        }
        return false;
    }
    
    @Override
    public List<ETLResultDTO> getUserJobs(String usuarioId) {
        // Implementar lógica para obtener trabajos de la BD
        return List.of();
    }
    
    @Override
    public CompletableFuture<ETLResultDTO> reprocessFailed(String jobId, List<Long> failedFolios) {
        // Implementar reprocesamiento de facturas fallidas
        log.info("Reprocessing {} failed folios for job {}", failedFolios.size(), jobId);
        return CompletableFuture.completedFuture(null);
    }

    // Métodos de apoyo
    private ETLProgressDTO initializeProgress(String jobId, String usuarioId, String fileName) {
        ETLProgressDTO progress = ETLProgressDTO.builder()
            .jobId(jobId)
            .usuarioId(usuarioId)
            .fileName(fileName)
            .status(ETLStatus.STARTED)
            .progress(0)
            .startTime(LocalDateTime.now())
            .build();
        
        jobProgressMap.put(jobId, progress);
        return progress;
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ETLException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") 
                && !contentType.equals("application/vnd.ms-excel"))) {
            throw new ETLException("Invalid file type. Only Excel files are allowed");
        }
        
        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB
            throw new ETLException("File size exceeds maximum allowed size (100 MB)");
        }
    }
    
    private void validateFacturas(List<FacturaDTO> facturas) {
        if (facturas == null || facturas.isEmpty()) {
            throw new ETLException("No valid data found in file");
        }
        
        // Validaciones adicionales de negocio
        long duplicateFolios = facturas.stream()
            .map(FacturaDTO::getFolio)
            .filter(folio -> facturas.stream().filter(f -> f.getFolio().equals(folio)).count() > 1)
            .count();
        
        if (duplicateFolios > 0) {
            log.warn("Found {} duplicate folios in file", duplicateFolios);
        }
    }
    
    private LoadResult loadInBatches(List<FacturaDTO> facturas, ETLProgressDTO progress) {
        LoadResult totalResult = new LoadResult(0, 0, 0, new java.util.ArrayList<>());
        int totalRecords = facturas.size();
        
        for (int i = 0; i < totalRecords; i += batchSize) {
            int end = Math.min(i + batchSize, totalRecords);
            List<FacturaDTO> batch = facturas.subList(i, end);
            
            progress.setCurrentStep(String.format("Cargando lote %d de %d", (i / batchSize) + 1, 
                                   (int) Math.ceil((double) totalRecords / batchSize)));
            
            LoadResult batchResult = loader.load(batch);
            
            // Acumular resultados
            totalResult = new LoadResult(
                totalResult.getInserted() + batchResult.getInserted(),
                totalResult.getUpdated() + batchResult.getUpdated(),
                totalResult.getSkipped() + batchResult.getSkipped(),
                totalResult.getErrors()
            );
            totalResult.getErrors().addAll(batchResult.getErrors());
            
            // Actualizar progreso
            int progressPercent = (int) (((double) end / totalRecords) * 100);
            progress.setProgress(progressPercent);
            progress.setRecordsLoaded(end);
        }
        
        return totalResult;
    }
    
    private ETLResultDTO buildResult(String jobId, ETLProgressDTO progress, LoadResult result) {
        return ETLResultDTO.builder()
            .jobId(jobId)
            .status(progress.getStatus())
            .fileName(progress.getFileName())
            .totalRecordsExtracted(progress.getRecordsExtracted())
            .totalRecordsTransformed(progress.getRecordsTransformed())
            .totalRecordsLoaded(result.getInserted())
            .totalRecordsUpdated(result.getUpdated())
            .totalRecordsSkipped(result.getSkipped())
            .totalErrors(result.getErrors().size())
            .errors(result.getErrors())
            .startTime(progress.getStartTime())
            .endTime(LocalDateTime.now())
            .build();
    }
}
