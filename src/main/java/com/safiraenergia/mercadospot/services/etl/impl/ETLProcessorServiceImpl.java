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
import com.safiraenergia.mercadospot.utils.ETLLogger;
import com.safiraenergia.mercadospot.services.etl.IETLProcessorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final ETLLogger etlLogger;

    @Value("${etl.batch.size:100}")
    private int batchSize;

    // Almacenamiento de trabajos en progreso
    private final Map<String, ETLProgressDTO> jobProgressMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ETLResultDTO>> jobFutureMap = new ConcurrentHashMap<>();

    @Autowired
    public ETLProcessorServiceImpl(ExtractorFactory extractorFactory, DataTransformer transformer, DataLoader loader, IFacturaService facturaService, ETLLogger etlLogger) {
        this.extractorFactory = extractorFactory;
        this.transformer = transformer;
        this.loader = loader;
        this.facturaService = facturaService;
        this.etlLogger = etlLogger;
    }
    
    @Override
    public CompletableFuture<ETLResultDTO> processExcelFile(MultipartFile file, String usuarioId) {
        String jobId = UUID.randomUUID().toString();
        
        // Leer el contenido del archivo ANTES de iniciar el hilo asíncrono
        byte[] fileContent;
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        try {
            fileContent = file.getBytes(); // Leer todo el contenido ahora
            etlLogger.logInfo(jobId, "Archivo leído exitosamente: " + fileName + " (" + fileContent.length + " bytes)");
        } catch (IOException e) {
            etlLogger.logError(jobId, "Error al leer el archivo", e);
            return CompletableFuture.completedFuture(createErrorResult(jobId, "Error al leer el archivo: " + e.getMessage()));
        }
        
        // Procesar asíncronamente con los bytes del archivo
        return processExcelFileAsync(jobId, fileContent, contentType, fileName, usuarioId);
    }

    @Async("etlTaskExecutor")
    public CompletableFuture<ETLResultDTO> processExcelFileAsync(String jobId, byte[] fileContent, String contentType, String fileName, String usuarioId) {
        ETLProgressDTO progress = initializeProgress(jobId, usuarioId, fileName);
        
        try {
            // 1. Validar archivo
            validateFile(fileContent, contentType, fileName);
            progress.setStatus(ETLStatus.VALIDATING);
            progress.setCurrentStep("Validando archivo...");
            
            // 2. Extraer datos
            progress.setStatus(ETLStatus.EXTRACTING);
            progress.setCurrentStep("Extrayendo datos del Excel...");
            
            DataExtractor extractor = extractorFactory.getExtractor(contentType);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
            List<Map<String, Object>> rawData = extractor.extract(inputStream);
            progress.setRecordsExtracted(rawData.size());
            etlLogger.logInfo(jobId, "Extraídos " + rawData.size() + " registros del Excel");
            
            // 3. Transformar datos
            progress.setStatus(ETLStatus.TRANSFORMING);
            progress.setCurrentStep("Transformando datos...");
            List<FacturaDTO> facturas = transformer.transform(rawData, jobId);
            progress.setRecordsTransformed(facturas.size());
            etlLogger.logInfo(jobId, "Transformados " + facturas.size() + " registros a FacturaDTO");
            
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
            
            etlLogger.logSuccess(jobId, "ETL completado exitosamente", result.getInserted());
            log.info("ETL job {} completed successfully. Inserted: {}, Errors: {}", 
                     jobId, result.getInserted(), result.getErrors().size());
            
            return CompletableFuture.completedFuture(finalResult);
            
        } catch (Exception e) {
            log.error("ETL job {} failed", jobId, e);
            etlLogger.logError(jobId, "ETL job failed", e);
            progress.setStatus(ETLStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            
            ETLResultDTO errorResult = createErrorResult(jobId, e.getMessage());
            
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            jobProgressMap.put(jobId, progress);
        }
    }

    private void validateFile(byte[] fileContent, String contentType, String fileName) {
        if (fileContent == null || fileContent.length == 0) {
            throw new ETLException("File is empty");
        }
        
        if (contentType == null || (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") 
                && !contentType.equals("application/vnd.ms-excel"))) {
            throw new ETLException("Invalid file type: " + contentType + ". Only Excel files are allowed");
        }
        
        if (fileContent.length > 100 * 1024 * 1024) { // 100 MB
            throw new ETLException("File size exceeds maximum allowed size (100 MB)");
        }
        
        log.info("File validated - Name: {}, Type: {}, Size: {} bytes", fileName, contentType, fileContent.length);
    }

    private ETLResultDTO createErrorResult(String jobId, String errorMessage) {
        return ETLResultDTO.builder()
            .jobId(jobId)
            .status(ETLStatus.FAILED)
            .errorMessage(errorMessage)
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now())
            .build();
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

        // contamos facturas validas (con folio > 0)
        long validFacturas = facturas.stream()
            .filter(f -> f.getFolio() != null && f.getFolio() > 0)
            .count();

        if (validFacturas == 0) {
            throw new ETLException("No valid facturas found in file. Check that folio numbers are present.");
        }

        log.info("Found {} valid facturas out of {} total", validFacturas, facturas.size());

        // Validaciones adicionales de negocio
        Map<Long, Long> duplicateMap = facturas.stream()
            .filter(f -> f.getFolio() != null && f.getFolio() > 0)
            .collect(Collectors.groupingBy(FacturaDTO::getFolio, Collectors.counting()));
            
        // Validaciones adicionales de negocio
        long duplicateFolios = duplicateMap.values().stream().filter(count -> count > 1).count();

        if (duplicateFolios > 0) {
            log.warn("Found {} duplicate folios in file", duplicateFolios);
        }
    }
    
    private LoadResult loadInBatches(List<FacturaDTO> facturas, ETLProgressDTO progress) {

        // filtramos facturas invalidas antes de cargar
        List<FacturaDTO> validFacturas = facturas.stream()
            .filter(f -> f.getFolio() != null && f.getFolio() > 0)
            .collect(Collectors.toList());
        
        if(validFacturas.isEmpty()){
            log.warn("No valid facturas to load after filtering");
            return new LoadResult(0, 0, facturas.size(), List.of("No valid facturas found"));
        }
        
        log.info("Loading {} valid facturas out of {} total", validFacturas.size(), facturas.size());
        return loader.load(validFacturas);
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
