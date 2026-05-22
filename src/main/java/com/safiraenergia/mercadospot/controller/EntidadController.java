package com.safiraenergia.mercadospot.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.entidad.EntidadDTO;
import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.services.entidad.IEntidadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("api/v1/entidad")
@Tag(name = "Entidad Controller", description = "Endpoints para gestión de entidades (Deudor y Acreedor)")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.DELETE, RequestMethod.POST, RequestMethod.PUT}, maxAge = 3600)
public class EntidadController {
    
    private final IEntidadService entidadService;

    @Autowired
    public EntidadController(IEntidadService entidadService) {
        this.entidadService = entidadService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todas las entidades")
    public ResponseEntity<List<EntidadDTO>> getAllEntidades() {
        log.info("Obteniendo todas las entidades");

        List<Entidad> entidades = entidadService.getAllEntidades();

        return ResponseEntity.ok(entidades.stream().map(this::converToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener entidad por ID")
    public ResponseEntity<EntidadDTO> getEntidadById(@PathVariable Long id) {
        log.info("Obteniendo entidad con ID: {}", id);
        Entidad entidad = entidadService.getEntidadById(id);
        return ResponseEntity.ok(converToDTO(entidad));
    }

    @GetMapping("/rut/{rut}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener entidad por RUT")
    public ResponseEntity<EntidadDTO> getEntidadByRut(@PathVariable String rut) {
        log.info("Obteniendo entidad por RUT: {}", rut);
        return entidadService.getEntidadByRut(rut)
            .map(this::converToDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/deudores")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todas las entidades deudoras")
    public ResponseEntity<List<EntidadDTO>> getDeudores() {
        log.info("Obteniendo entidades deudoras");
        List<Entidad> entidades = entidadService.getAllEntidades();

        // filtramos entidades que son deudoras (tienen tipo DEUDOR)
        return ResponseEntity.ok(entidades.stream()
            .filter(e -> e.getTipoEntidad().stream().anyMatch(t -> "Deudor".equals(t.getTipoRol())))
            .map(this::converToDTO)
            .collect(Collectors.toList()));
    }

    @GetMapping("/acreedores")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todas las entidades acreedoras")
    public ResponseEntity<List<EntidadDTO>> getAcreedores(){
        log.info("Obteniendo entidades acreedoras");

        List<Entidad> entidades = entidadService.getAllEntidades();

        // filtramos entidades que son acreedoras
        return ResponseEntity.ok(entidades.stream()
            .filter(e -> e.getTipoEntidad().stream().anyMatch(t -> "Acreedor".equals(t.getTipoRol())))
            .map(this::converToDTO)
            .collect(Collectors.toList()));
    }

    @PostMapping
    @Operation(summary = "Crear nueva entidad")
    public ResponseEntity<EntidadDTO> createEntidad(@RequestBody EntidadDTO entidadDTO) {
        log.info("Creando entidad con RUT: {}", entidadDTO.getRutEntidad());

        Entidad entidad = Entidad.builder()
            .rutEntidad(entidadDTO.getRutEntidad())
            .nombre(entidadDTO.getNombre())
            .build();
        
        Entidad saved = entidadService.createEntidad(entidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(converToDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar entidad")
    public ResponseEntity<EntidadDTO> updateEntidad(@PathVariable Long id, @RequestBody EntidadDTO entidadDTO) {
        log.info("Actualizando entidad con ID: {}", id);
        
        Entidad entidad = Entidad.builder()
            .rutEntidad(entidadDTO.getRutEntidad())
            .nombre(entidadDTO.getNombre())
            .build();
        
        Entidad updated = entidadService.updateEntidad(id, entidad);
        return ResponseEntity.ok(converToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar entidad")
    public ResponseEntity<Void> deleteEntidad(@PathVariable Long id) {
        log.info("Eliminando entidad con ID: {}", id);
        entidadService.deleteEntidad(id);
        return ResponseEntity.noContent().build();
    }

    private EntidadDTO converToDTO(Entidad entidad) {
        return EntidadDTO.builder()
            .id(entidad.getId())
            .nombre(entidad.getNombre())
            .rutEntidad(entidad.getRutEntidad())
            .tiposEntidad(entidad.getTipoEntidad().stream()
                    .map(t -> t.getTipoRol())
                    .collect(Collectors.toList()))
            .build();
    }
}
