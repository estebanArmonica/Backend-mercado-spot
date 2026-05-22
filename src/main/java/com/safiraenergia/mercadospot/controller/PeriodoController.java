package com.safiraenergia.mercadospot.controller;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.periodo.PeriodoDTO;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.services.periodo.IPeriodoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/periodo")
@CrossOrigin(origins = "*", maxAge = 3600, methods = {RequestMethod.DELETE, RequestMethod.POST, RequestMethod.GET})
@Tag(name = "Periodos", description = "Endpoints para gestión de periodos")
public class PeriodoController {
    
    private final IPeriodoService periodoService;

    @Autowired
    public PeriodoController(IPeriodoService periodoService){
        this.periodoService = periodoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todos los periodos")
    public ResponseEntity<List<PeriodoDTO>> getAllPeriodos() {
        log.info("Obteniendo todos los periodos");
        List<Periodo> periodos = periodoService.getAllPeriodos();
        return ResponseEntity.ok(periodos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener periodo por ID")
    public ResponseEntity<PeriodoDTO> getPeriodoById(@PathVariable Long id) {
        log.info("Obteniendo periodo con ID: {}", id);
        Periodo periodo = periodoService.getPeriodoById(id);
        return ResponseEntity.ok(convertToDTO(periodo));
    }

    @GetMapping("/year/{year}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener periodos por año")
    public ResponseEntity<List<PeriodoDTO>> getPeriodosByYear(@PathVariable int year) {
        log.info("Obteniendo periodos por año: {}", year);
        List<Periodo> periodos = periodoService.getPeriodosByYear(year);
        return ResponseEntity.ok(periodos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener periodo actual")
    public ResponseEntity<PeriodoDTO> getCurrentPeriodo() {
        log.info("Obteniendo periodo actual");
        Periodo periodo = periodoService.getCurrentPeriodo();
        return ResponseEntity.ok(convertToDTO(periodo));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ETL_OPERATOR')")
    @Operation(summary = "Crear nuevo periodo")
    public ResponseEntity<PeriodoDTO> createPeriodo(@RequestBody PeriodoDTO periodoDTO) {
        log.info("Creando periodo para fecha: {}", periodoDTO.getMes());
        
        Periodo periodo = Periodo.builder()
            .mes(periodoDTO.getMes())
            .build();
        
        Periodo saved = periodoService.createPeriodo(periodo);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar periodo")
    public ResponseEntity<Void> deletePeriodo(@PathVariable Long id) {
        log.info("Eliminando periodo con ID: {}", id);
        periodoService.deletePeriodo(id);
        return ResponseEntity.noContent().build();
    }

    private PeriodoDTO convertToDTO(Periodo periodo) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodo.getMes());
        
        return PeriodoDTO.builder()
            .id(periodo.getId())
            .mes(periodo.getMes())
            .year(cal.get(Calendar.YEAR))
            .month(cal.get(Calendar.MONTH) + 1)
            .build();
    }
}
