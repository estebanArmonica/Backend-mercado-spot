package com.safiraenergia.mercadospot.controllers;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.safiraenergia.mercadospot.controller.EtlController;
import com.safiraenergia.mercadospot.dto.etl.ETLProgressDTO;
import com.safiraenergia.mercadospot.dto.etl.ETLResultDTO;
import com.safiraenergia.mercadospot.enums.ETLStatus;
import com.safiraenergia.mercadospot.services.etl.IETLProcessorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(EtlController.class)
@DisplayName("Pruebas del controllador del ETL")
public class TestEtlController {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IETLProcessorService etlProcessorService;

    private MockMultipartFile mockFile;

    /**
     * Creamos las variables de archivo excel el cual utilizaremos para la carga de ETL 
    */
    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
            "file",
            "test-data.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test content".getBytes()
        );
    }

    /**
     * este Testing nos devuelve una carga ETL en estado de PROCESSING donde realiza
     * la carga correspondiente de un archivo excel
     * @throws Exception
    */
    @Test
    @DisplayName("Debe aceptar la carga de un archivo excel")
    void shouldAcceptExcelUpload() throws Exception {
        ETLResultDTO result = ETLResultDTO.builder()
            .jobId("test-job-123")
            .status(ETLStatus.STARTED)
            .startTime(LocalDateTime.now())
            .build();
        
        when(etlProcessorService.processExcelFile(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(result));
        
        mockMvc.perform(multipart("/api/v1/etl/upload")
                .file(mockFile)
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Archivo recibido. Procesamiento iniciado"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    /**
     * Este Testing devolvera el progreso de una carga ETL donde podremos realizar seguimiento
     * para saber como está avanzando la carga
     * @throws Exception: lanzara errores en caso de ser necesario
    */
    @Test
    @DisplayName("Debe devolver el progreso de un trabajo ETL")
    void shouldGetJobProgress() throws Exception {
        ETLProgressDTO progress = ETLProgressDTO.builder()
            .jobId("test-job-123")
            .status(ETLStatus.EXTRACTING)
            .progress(45)
            .recordsExtracted(500)
            .recordsTransformed(450)
            .currentStep("Extrayendo datos...")
            .build();
        
        when(etlProcessorService.getJobProgress("test-job-123"))
            .thenReturn(progress);
        
        mockMvc.perform(get("/api/v1/etl/progress/test-job-123")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-123"))
                .andExpect(jsonPath("$.progress").value(45))
                .andExpect(jsonPath("$.recordsExtracted").value(500));
    }

    /**
     * Este Testing nos devuelve una cancelación de un proceso ETL que esté en progreso
     * esto sirve en caso de ser un archivo equivocado o durante el proceso haya realizado problemas
     * @throws Exception
    */
    @Test
    @DisplayName("Debe cancelar un trabajo ETL en progreso")
    void shouldCancelETLJob() throws Exception {
        when(etlProcessorService.cancelJob("test-job-123"))
            .thenReturn(true);
        
        mockMvc.perform(delete("/api/v1/etl/cancel/test-job-123")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trabajo cancelado exitosamente"));
    }
    
    /**
     * nos devolvera un 404 en caso de no poder cancelar un trabajo ETL existente
     * @throws Exception: en estos casos lanzara una excepción para conocer el problema
    */
    @Test
    @DisplayName("Debe devolver 404 al cancelar un trabajo inexistente")
    void shouldReturn404WhenCancelingNonExistentJob() throws Exception {
        when(etlProcessorService.cancelJob("non-existent"))
            .thenReturn(false);
        
        mockMvc.perform(delete("/api/v1/etl/cancel/non-existent")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se pudo cancelar el trabajo o no existe"));
    }
}
