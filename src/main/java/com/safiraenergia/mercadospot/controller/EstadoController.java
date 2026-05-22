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
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.estado.EstadoDTO;
import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.services.estado.IEstadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/estados")
@CrossOrigin(origins = "*", maxAge = 3600, methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
@Tag(name = "Estados", description = "Endpoints para gestión de estados de factura")
public class EstadoController {
    
    private final IEstadoService estadoService;

    @Autowired
    public EstadoController(IEstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todos los estados")
    public ResponseEntity<List<EstadoDTO>> getAllEstados() {
        log.info("Obteniendo todos los estados");
        List<Estado> estados = estadoService.getAllEstados();
        return ResponseEntity.ok(estados.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener estado por ID")
    public ResponseEntity<EstadoDTO> getEstadoById(@PathVariable Long id) {
        log.info("Obteniendo estado con ID: {}", id);
        Estado estado = estadoService.getEstadoById(id);
        return ResponseEntity.ok(convertToDTO(estado));
    }

    @GetMapping("/descripcion/{descripcion}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener estado por descripción")
    public ResponseEntity<EstadoDTO> getEstadoByDescripcion(@PathVariable String descripcion) {
        log.info("Obteniendo estado por descripción: {}", descripcion);
        return estadoService.getEstadoByDescripcion(descripcion)
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear nuevo estado")
    public ResponseEntity<EstadoDTO> createEstado(@RequestBody EstadoDTO estadoDTO) {
        log.info("Creando estado: {}", estadoDTO.getDescripcion());
        
        Estado estado = Estado.builder()
            .descripcion(estadoDTO.getDescripcion())
            .build();
        
        Estado saved = estadoService.createEstado(estado);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado")
    public ResponseEntity<EstadoDTO> updateEstado(
            @PathVariable Long id,
            @RequestBody EstadoDTO estadoDTO) {
        log.info("Actualizando estado con ID: {}", id);
        
        Estado estado = Estado.builder()
            .descripcion(estadoDTO.getDescripcion())
            .build();
        
        Estado updated = estadoService.updateEstado(id, estado);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar estado")
    public ResponseEntity<Void> deleteEstado(@PathVariable Long id) {
        log.info("Eliminando estado con ID: {}", id);
        estadoService.deleteEstado(id);
        return ResponseEntity.noContent().build();
    }

    private EstadoDTO convertToDTO(Estado estado) {
        return EstadoDTO.builder()
            .id(estado.getId())
            .descripcion(estado.getDescripcion())
            .build();
    }
}
