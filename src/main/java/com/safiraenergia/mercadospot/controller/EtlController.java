package com.safiraenergia.mercadospot.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.safiraenergia.mercadospot.dto.etl.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.etl.ETLResultDTO;
import com.safiraenergia.mercadospot.services.etl.IETLProcessorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/etl")
@Tag(name = "ETL Controller", description = "Endpoints para procesamiento de archivos excel")
@CrossOrigin(origins = "*", methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST}, maxAge = 3600)
public class EtlController {

    private final IETLProcessorService etlProcessorService;

    @Autowired
    public EtlController(IETLProcessorService etlProcessorService) {
        this.etlProcessorService = etlProcessorService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Subir y procesa archivos excel",
        description = "Procesa un archivo excel con hojas Deudor y Acreedor y carga los datos en la base de datos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Procesamiento iniciado"),
        @ApiResponse(responseCode = "400", description = "Archivo invalido"),
        @ApiResponse(responseCode = "401", description = "No Autorizado"),
    })
    public ResponseEntity<Map<String, String>> uploadExcel(@Parameter(description = "Archivo Excel a procesar", required = true)
                                                           @RequestParam("file") MultipartFile file, Principal principal){
        
        log.info("Recibida solicitud de carga ETL de usuario: {}", principal.getName());

        CompletableFuture<ETLResultDTO> future = etlProcessorService.processExcelFile(file, principal.getName());

        System.out.println(future);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Archivo recibido. Procesamiento iniciado");
        response.put("status", "PROCESSING");

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/progress/{jobId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener progreso del trabajo ETL")
    public ResponseEntity<ETLProgressDTO> getProgress(@PathVariable String jobId) {
        log.info("Consultando progreso del job: {}", jobId);
        ETLProgressDTO progress = etlProcessorService.getJobProgress(jobId);
        return ResponseEntity.ok(progress);
    }

    @DeleteMapping("/cancel/{jobId}")
    @Operation(summary = "Cancelar trabajo ETL en progreso")
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable String jobId) {
        log.info("Cancelando job: {}", jobId);
        boolean cancelled = etlProcessorService.cancelJob(jobId);

        Map<String, String> response = new HashMap<>();
        if(cancelled) {
            response.put("message", "Trabajo cancelado exitosamente");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "No se pudo cancelar el trabajo o no existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
