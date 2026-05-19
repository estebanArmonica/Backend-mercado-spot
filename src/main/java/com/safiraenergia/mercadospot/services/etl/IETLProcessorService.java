package com.safiraenergia.mercadospot.services.etl;

import org.springframework.web.multipart.MultipartFile;

import com.safiraenergia.mercadospot.dto.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.ETLResultDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz para el servicio de procesamiento ETL
 * Aplicando Principios de Segregación de Interfaces (ISP)
*/
public interface IETLProcessorService {
    
    /**
     * Procesa el archivo Excel de forma asíncronica
    */
    CompletableFuture<ETLResultDTO> processExcelFile(MultipartFile file, String usuarioId);

    /**
     * Obtiene el progreso de un trabajo ETL
    */
    ETLProgressDTO getJobProgress(String jobId);
    
    /**
     * Cancela un trabajo ETL en progreso
    */
    boolean cancelJob(String jobId);
    
    /**
     * Obtiene todos los trabajos de un usuario
    */
    List<ETLResultDTO> getUserJobs(String usuarioId);
    
    /**
     * Reprocesa facturas fallidas
    */
    CompletableFuture<ETLResultDTO> reprocessFailed(String jobId, List<Long> failedFolios);
}
