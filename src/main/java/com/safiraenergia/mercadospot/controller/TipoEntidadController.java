package com.safiraenergia.mercadospot.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.tipoentidad.TipoEntidadDTO;
import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.services.tipoEntidad.ITipoEntidadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/tipos-entidad")
@CrossOrigin(origins = "*", maxAge = 3600, methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
@Tag(name = "Tipos de Entidad", description = "Endpoints para gestión de tipos de entidad (DEUDOR, ACREEDOR, etc.)")
public class TipoEntidadController {
    
    private final ITipoEntidadService tipoEntidadService;

    @Autowired
    public TipoEntidadController(ITipoEntidadService tipoEntidadService) {
        this.tipoEntidadService = tipoEntidadService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todos los tipos de entidad")
    public ResponseEntity<List<TipoEntidadDTO>> getAllTipoEntidades() {
        log.info("Obteniendo todos los tipos de entidad");
        List<TipoEntidad> tipos = tipoEntidadService.getAllTipoEntidades();
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/ordenados")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tipos de entidad ordenados")
    public ResponseEntity<List<TipoEntidadDTO>> getTipoEntidadesOrdenadas() {
        log.info("Obteniendo tipos de entidad ordenados");
        List<TipoEntidad> tipos = tipoEntidadService.getTipoEntidadesOrdenadas();
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tipo de entidad por ID")
    public ResponseEntity<TipoEntidadDTO> getTipoEntidadById(@PathVariable Long id) {
        log.info("Obteniendo tipo de entidad con ID: {}", id);
        TipoEntidad tipo = tipoEntidadService.getTipoEntidadById(id);
        return ResponseEntity.ok(convertToDTO(tipo));
    }

    @GetMapping("/rol/{tipoRol}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tipo de entidad por rol")
    public ResponseEntity<TipoEntidadDTO> getTipoEntidadByTipoRol(@PathVariable String tipoRol) {
        log.info("Obteniendo tipo de entidad por rol: {}", tipoRol);
        return tipoEntidadService.getTipoEntidadByTipoRol(tipoRol)
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar tipos de entidad por rol")
    public ResponseEntity<List<TipoEntidadDTO>> searchTipoEntidades(@RequestParam String keyword) {
        log.info("Buscando tipos de entidad por: {}", keyword);
        List<TipoEntidad> tipos = tipoEntidadService.getTipoEntidadesByTipoRolContaining(keyword);
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/deudor")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tipo DEUDOR")
    public ResponseEntity<TipoEntidadDTO> getTipoDeudor() {
        log.info("Obteniendo tipo DEUDOR");
        return tipoEntidadService.getTipoEntidadByTipoRol("DEUDOR")
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new RuntimeException("Tipo DEUDOR no encontrado"));
    }

    @GetMapping("/acreedor")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tipo ACREEDOR")
    public ResponseEntity<TipoEntidadDTO> getTipoAcreedor() {
        log.info("Obteniendo tipo ACREEDOR");
        return tipoEntidadService.getTipoEntidadByTipoRol("ACREEDOR")
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new RuntimeException("Tipo ACREEDOR no encontrado"));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo tipo de entidad")
    public ResponseEntity<TipoEntidadDTO> createTipoEntidad(@RequestBody TipoEntidadDTO tipoEntidadDTO) {
        log.info("Creando tipo de entidad: {}", tipoEntidadDTO.getTipoRol());
        
        TipoEntidad tipo = TipoEntidad.builder()
            .tipoRol(tipoEntidadDTO.getTipoRol().toUpperCase())
            .build();
        
        TipoEntidad saved = tipoEntidadService.createTipoEntidad(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de entidad")
    public ResponseEntity<TipoEntidadDTO> updateTipoEntidad(
            @PathVariable Long id,
            @RequestBody TipoEntidadDTO tipoEntidadDTO) {
        log.info("Actualizando tipo de entidad con ID: {}", id);
        
        TipoEntidad tipo = TipoEntidad.builder()
            .tipoRol(tipoEntidadDTO.getTipoRol().toUpperCase())
            .build();
        
        TipoEntidad updated = tipoEntidadService.updateTipoEntidad(id, tipo);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tipo de entidad")
    public ResponseEntity<Void> deleteTipoEntidad(@PathVariable Long id) {
        log.info("Eliminando tipo de entidad con ID: {}", id);
        tipoEntidadService.deleteTipoEntidad(id);
        return ResponseEntity.noContent().build();
    }

    private TipoEntidadDTO convertToDTO(TipoEntidad tipo) {
        return TipoEntidadDTO.builder()
            .id(tipo.getId())
            .tipoRol(tipo.getTipoRol())
            .build();
    }
}
