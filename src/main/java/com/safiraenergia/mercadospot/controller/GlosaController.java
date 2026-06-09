package com.safiraenergia.mercadospot.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.safiraenergia.mercadospot.dto.glosa.GlosaDTO;
import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.services.glosa.IGlosaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/glosa")
@CrossOrigin(origins = "http://localhost:4200/", maxAge = 3600, methods = {RequestMethod.DELETE, RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT})
@Tag(name = "Glosas", description = "Endpoints para gestión de glosas")
public class GlosaController {
    
    private final IGlosaService glosaService;

    @Autowired
    public GlosaController(IGlosaService glosaService) {
        this.glosaService = glosaService;
    }

    @GetMapping("/list-all")
    @Operation(summary = "Obtener todas las glosas")
    public ResponseEntity<List<GlosaDTO>> getAllGlosas() {
        log.info("Obteniendo todas las glosas");
        List<Glosa> glosas = glosaService.getAllGlosas();
        return ResponseEntity.ok(glosas.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/list-glosa/{id}")
    @Operation(summary = "Obtener glosa por ID")
    public ResponseEntity<GlosaDTO> getGlosaById(@PathVariable Long id) {
        log.info("Obteniendo glosa con ID: {}", id);
        Glosa glosa = glosaService.getGlosaById(id);
        return ResponseEntity.ok(convertToDTO(glosa));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar glosas por descripción")
    public ResponseEntity<List<GlosaDTO>> searchGlosas(@RequestParam String keyword) {
        log.info("Buscando glosas por: {}", keyword);
        List<Glosa> glosas = glosaService.getGlosasByDescripcionContaining(keyword);
        return ResponseEntity.ok(glosas.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/create-new-glosa")
    @Operation(summary = "Crear nueva glosa")
    public ResponseEntity<GlosaDTO> createGlosa(@RequestBody GlosaDTO glosaDTO) {
        log.info("Creando glosa: {}", glosaDTO.getDescripcion());
        
        Glosa glosa = Glosa.builder()
            .descripcion(glosaDTO.getDescripcion())
            .build();
        
        Glosa saved = glosaService.createGlosa(glosa);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @PutMapping("/update-glosa/{id}")
    @Operation(summary = "Actualizar glosa")
    public ResponseEntity<GlosaDTO> updateGlosa(
            @PathVariable Long id,
            @RequestBody GlosaDTO glosaDTO) {
        log.info("Actualizando glosa con ID: {}", id);
        
        Glosa glosa = Glosa.builder()
            .descripcion(glosaDTO.getDescripcion())
            .build();
        
        Glosa updated = glosaService.updateGlosa(id, glosa);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    @DeleteMapping("/delete-glosa/{id}")
    @Operation(summary = "Eliminar glosa")
    public ResponseEntity<Void> deleteGlosa(@PathVariable Long id) {
        log.info("Eliminando glosa con ID: {}", id);
        glosaService.deleteGlosa(id);
        return ResponseEntity.noContent().build();
    }

    private GlosaDTO convertToDTO(Glosa glosa) {
        return GlosaDTO.builder()
            .id(glosa.getId())
            .descripcion(glosa.getDescripcion())
            .build();
    }
}
