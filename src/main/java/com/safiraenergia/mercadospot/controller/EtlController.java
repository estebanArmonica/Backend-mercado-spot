package com.safiraenergia.mercadospot.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.safiraenergia.mercadospot.dto.etl.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.etl.ETLResultDTO;
import com.safiraenergia.mercadospot.enums.ETLStatus;
import com.safiraenergia.mercadospot.services.etl.IETLProcessorService;
import com.safiraenergia.mercadospot.utils.ETLLogger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/etl")
@RequiredArgsConstructor
@Tag(name = "ETL", description = "Endpoints para procesamiento de archivos excel")
public class EtlController {

    private final IETLProcessorService etlProcessorService;
    private final ETLLogger etlLogger;

    // para almacenar emitters activos por jobId (esto ayuda al SSE)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // almacenamos los logs usando el jobId
    private final Map<String, StringBuilder> jobLogs = new ConcurrentHashMap<>();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Subir y procesa archivos excel",
        description = "Procesa un archivo excel con hojas Deudor y Acreedor y carga los datos en la base de datos",
        tags = {"ETL"},
        responses = {
            @ApiResponse(responseCode = "202", description = "Procesamiento iniciado"),
            @ApiResponse(responseCode = "400", description = "Archivo invalido"),
            @ApiResponse(responseCode = "401", description = "No Autorizado"),
        }
    )
    public ResponseEntity<Map<String, String>> uploadExcel(@Parameter(description = "Archivo Excel a procesar", required = true) @RequestParam("file") MultipartFile file, Principal principal){
        
        log.info("Recibida solicitud de carga ETL de usuario: {}", principal.getName());

        // Generamos el jobId antes de poder realizar el proceso de ETL
        String jobId = UUID.randomUUID().toString(); // generamos un jobId de Sring de forma random para cada Job de trabajo

        // inicializamos los logs para este job
        jobLogs.put(jobId, new StringBuilder());
        addLogToJob(jobId, "INFO", " Iniciando proceso ETL");
        addLogToJob(jobId, "INFO", " Archivo: " + file.getOriginalFilename());
        addLogToJob(jobId, "INFO", " Tamaño: " + (file.getSize() / 1024) + " KB");
        addLogToJob(jobId, "INFO", "Job ID: " + jobId);

        CompletableFuture<ETLResultDTO> future = etlProcessorService.processExcelFile(file, principal.getName(), jobId);

        // procesamos el resultado cuando termine
        future.thenAccept(result -> {
            if (result.getStatus() == ETLStatus.COMPLETED) {
                addLogToJob(jobId, "SUCCESS", "ETL completado exitosamente");
                addLogToJob(jobId, "INFO", "Registros insertados: " + result.getTotalRecordsLoaded());
                addLogToJob(jobId, "INFO", "Tiempo total: " + result.getEndTime() + " ms");
            } else {
                addLogToJob(jobId, "ERROR", "ETL falló: " + result.getErrorMessage());
            }

            // cerramos el emitters cuando termine
            closeEmitter(jobId);
        }).exceptionally(ex -> {
            addLogToJob(jobId, "ERROR", "❌ Error inesperado: " + ex.getMessage());
            closeEmitter(jobId);
            return null;
        });

        Map<String, String> response = new HashMap<>();
        response.put("message", "Archivo recibido. Procesamiento iniciado");
        response.put("status", "PROCESSING");
        response.put("jobId", jobId);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/progress/{jobId}")
    @Operation(
        summary = "Obtener progreso del trabajo ETL",
        description = "Obtiene el progreso del job mientras este en proceso de migración",
        tags = {"ETL"},
        parameters = {
            @Parameter(
                name = "jobId",
                description = "El jobId es requerido para mostrar y conocer el progreso de una migracion en proceso de ETL",
                example = "ajsdbdsfsdiws",
                required = true
            )
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Mostrando progreso del ETL"),
            @ApiResponse(responseCode = "400", description = "Proceso de ETL no existe para mostrar"),
            @ApiResponse(responseCode = "401", description = "Error de credenciales invalidas"),
        }
    )
    public ResponseEntity<ETLProgressDTO> getProgress(@PathVariable String jobId) {
        log.info("Consultando progreso del job: {}", jobId);
        ETLProgressDTO progress = etlProcessorService.getJobProgress(jobId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/logs/{jobId}")
    @Operation(
        summary = "Stream de logs en tiempo real",
        description = "Mantiene una conexión abierta para enviar logs en tiempo real del proceso ETL",
        tags = {"ETL"}
    )
    public SseEmitter streamLogs(@PathVariable String jobId) {
        log.info("Cliente conectado para logs del job: {}", jobId);

        SseEmitter emitter = new SseEmitter(600000L); // 10 minutos del timeout

        // guardamos el emitter
        emitters.put(jobId, emitter);

        // enviamos los logs existentes
        StringBuilder existingLogs = jobLogs.get(jobId);
        if(existingLogs != null && existingLogs.length() > 0) {
            try {
                for (String line : existingLogs.toString().split("\n")) {
                    if (!line.trim().isEmpty()) {
                        emitter.send(SseEmitter.event()
                            .name("log")
                            .data(line));
                    }
                }
            } catch (IOException e) {
                log.error("Error enviando logs existentes", e);
            }
        }

        // manejamos eventos
        emitter.onCompletion(() -> {
            log.info("Cliente desconectado del job: {}", jobId);
            emitters.remove(jobId);
        });

        emitter.onTimeout(() -> {
            log.warn("Timeout en conexión de logs para job: {}", jobId);
            emitters.remove(jobId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("Error en conexión de logs para job: {}", jobId, ex);
            emitters.remove(jobId);
        });

        return emitter;
    }

    @DeleteMapping("/cancel/{jobId}")
    @Operation(
        summary = "Cancelar trabajo ETL en progreso",
        description = "Cancelamos un proceso ETL mediando el jobId",
        tags = {"ETL"},
        parameters = {
            @Parameter(
                name = "jobId",
                description = "El jobId es requerido para mostrar y cancelar el progreso de una migracion en proceso de ETL",
                example = "ajsdbdsfsdiws",
                required = true
            )
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Cancelando progreso del ETL"),
            @ApiResponse(responseCode = "400", description = "Proceso de ETL no existe para mostrar"),
            @ApiResponse(responseCode = "401", description = "Error de credenciales invalidas"),
        }
    )
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable String jobId) {
        log.info("Cancelando job: {}", jobId);
        boolean cancelled = etlProcessorService.cancelJob(jobId);

        Map<String, String> response = new HashMap<>();
        if(cancelled) {
            addLogToJob(jobId, "WARNING", "Proceso cancelado por el usuario");
            response.put("message", "Trabajo cancelado exitosamente");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "No se pudo cancelar el trabajo o no existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // metodos para agregar logs y notificar a los clientes
    public void addLogToJob(String jobId, String level, String message) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        
        String logLine = String.format("[%s] [%s] %s", timestamp, level, message);

        // guardamos en la memoria
        StringBuilder logs = jobLogs.computeIfAbsent(jobId, k -> new StringBuilder());
        logs.append(logLine).append("\n");

        // Notificamos a los clientes conectados
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(logLine));
            } catch (IOException e) {
                log.warn("Error enviando log a cliente: {}", e.getMessage());
                emitters.remove(jobId);
            }
        }

        // agregamos un switch case para guiardar en archivo
        switch (level) {
            case "ERROR":
                etlLogger.logError(jobId, message, null);
                break;
            case "SUCCESS":
                etlLogger.logSuccess(jobId, message, 0);
                break;
            case "WARNING":
                etlLogger.logWarning(jobId, message);
                break;
            default:
                etlLogger.logInfo(jobId, message);
                break;
        }
    }

    private void closeEmitter(String jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        if(emitter != null) {
            emitter.complete();
        }
    }
}
