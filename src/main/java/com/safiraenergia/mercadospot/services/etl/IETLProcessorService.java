package com.safiraenergia.mercadospot.services.etl;

import org.springframework.web.multipart.MultipartFile;

import com.safiraenergia.mercadospot.dto.etl.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.etl.ETLResultDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz para el servicio de procesamiento ETL
 * Aplicando Principios de Segregación de Interfaces (ISP)
*/
public interface IETLProcessorService {
    
    /**
     * Procesa el Archivo Excel de forma asíncronica 
     * @param file: Archivo Excel
     * @param usuarioId: El ID del usuario quien realizo el job
     * @param jobId: El jobId unico del trabajo
     * @return
    */
    CompletableFuture<ETLResultDTO> processExcelFile(MultipartFile file, String usuarioId, String jobId);

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
