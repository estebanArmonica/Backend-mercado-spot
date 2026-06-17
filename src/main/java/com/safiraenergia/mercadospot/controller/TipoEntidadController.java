package com.safiraenergia.mercadospot.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.tipoentidad.TipoEntidadDTO;
import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.services.tipoEntidad.ITipoEntidadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/tipos-entidad")
@Tag(name = "Tipos de Entidad", description = "Endpoints para gestión de tipos de entidad (DEUDOR, ACREEDOR, etc.)")
public class TipoEntidadController {
    
    private final ITipoEntidadService tipoEntidadService;

    @Autowired
    public TipoEntidadController(ITipoEntidadService tipoEntidadService) {
        this.tipoEntidadService = tipoEntidadService;
    }

    /**
     * Creaciónes de APIs de tipo entidad 
     * @return retorna en cada uno un objeto JSON correspondiente
    */

    @GetMapping("/list-all-tipo-entidad")
    @Operation(
        summary = "Obtener todos los tipos de entidad",
        description = "Retorna un conjunto de todos los tipos de entidades disponibles.",
        tags = {"Tipos de Entidad"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de los tipos de entidad obtenido exitosamente",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }

    )
    public ResponseEntity<List<TipoEntidadDTO>> getAllTipoEntidades() {
        log.info("Obteniendo todos los tipos de entidad");
        List<TipoEntidad> tipos = tipoEntidadService.getAllTipoEntidades();
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }


    @GetMapping("/ordenados")
    @Operation(
        summary = "Obtener tipos de entidad ordenados",
        description = "Retona un conjunto de todos los tipos de entidades de forma ordenada (ASC o DESC)",
        tags = {"Tipos de Entidad"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de los tipos de entidad en forma ordenado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<List<TipoEntidadDTO>> getTipoEntidadesOrdenadas() {
        log.info("Obteniendo tipos de entidad ordenados");
        List<TipoEntidad> tipos = tipoEntidadService.getTipoEntidadesOrdenadas();
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/tipo-entidad/{id}")
    @Operation(
        summary = "Obtener tipo de entidad por ID",
        description = "Retorna un dato de un solo tipo de entidad a través de su ID",
        tags = {"Tipos de Entidad"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El ID es requerido para realizar la busqueda de un solo tipo de entidad",
                example = "1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad buscado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<TipoEntidadDTO> getTipoEntidadById(@PathVariable Long id) {
        log.info("Obteniendo tipo de entidad con ID: {}", id);
        TipoEntidad tipo = tipoEntidadService.getTipoEntidadById(id);
        return ResponseEntity.ok(convertToDTO(tipo));
    }

    @GetMapping("/rol/{tipoRol}")
    @Operation(
        summary = "Obtener tipo de entidad por rol",
        description = "Retorna un dato de un solo tipo de entidad a través de su nombre",
        tags = {"Tipos de Entidad"},
        parameters = {
            @Parameter(
                name = "tipo_rol",
                description = "El (tipo_rol) es requerido para realizar la busqueda de un solo tipo de entidad",
                example = "DEUDOR",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad buscado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<TipoEntidadDTO> getTipoEntidadByTipoRol(@PathVariable String tipoRol) {
        log.info("Obteniendo tipo de entidad por rol: {}", tipoRol);
        return tipoEntidadService.getTipoEntidadByTipoRol(tipoRol)
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(
        summary = "Buscar tipos de entidad por rol",
        description = "Retorna un dato de un solo tipo de entidad a través de su nombre",
        tags = {"Tipos de Entidad"},
        parameters = {
            @Parameter(
                name = "tipo_rol",
                description = "El (tipo_rol) es requerido para realizar la busqueda de un solo tipo de entidad",
                example = "DEUDOR",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad buscado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<List<TipoEntidadDTO>> searchTipoEntidades(@RequestParam String keyword) {
        log.info("Buscando tipos de entidad por: {}", keyword);
        List<TipoEntidad> tipos = tipoEntidadService.getTipoEntidadesByTipoRolContaining(keyword);
        return ResponseEntity.ok(tipos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/deudor")
    @Operation(
        summary = "Obtener tipo DEUDOR",
        description = "Retorna unicamente tipos de rol que sean DEUDOR",
        tags = {"Tipos de Entidad"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<TipoEntidadDTO> getTipoDeudor() {
        log.info("Obteniendo tipo DEUDOR");
        return tipoEntidadService.getTipoEntidadByTipoRol("DEUDOR")
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new RuntimeException("Tipo DEUDOR no encontrado"));
    }

    @GetMapping("/acreedor")
    @Operation(
        summary = "Obtener tipo ACREEDOR",
        description = "Retorna unicamente tipos de rol que sean ACREEDOR",
        tags = {"Tipos de Entidad"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay tipos de entidades para mostrar."
            )
        }
    )
    public ResponseEntity<TipoEntidadDTO> getTipoAcreedor() {
        log.info("Obteniendo tipo ACREEDOR");
        return tipoEntidadService.getTipoEntidadByTipoRol("ACREEDOR")
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new RuntimeException("Tipo ACREEDOR no encontrado"));
    }

    @PostMapping("/create-new-tipo-entidad")
    @Operation(
        summary = "Crear nuevo tipo de entidad",
        description = "Crea un nuevo tipo de entidad que vaya ingresado por el administrador",
        tags = {"Tipos de Entidad"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos requeridos: 'tipo_rol'",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TipoEntidad.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Tipo de entidad nuevo creado con exito."
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Error validación (e.g. campos o un campo está vacio)."
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Conflict (e.g. el tipo de entidad ya está creado o ya existe)."
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Sin permisos para realizar la petición."
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Error interno del servidor."
            ),
        }
    )
    public ResponseEntity<TipoEntidadDTO> createTipoEntidad(@RequestBody TipoEntidadDTO tipoEntidadDTO) {
        log.info("Creando tipo de entidad: {}", tipoEntidadDTO.getTipoRol());
        
        TipoEntidad tipo = TipoEntidad.builder()
            .tipoRol(tipoEntidadDTO.getTipoRol().toUpperCase())
            .build();
        
        TipoEntidad saved = tipoEntidadService.createTipoEntidad(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @PutMapping("/update-tipo-entidad/{id}")
    @Operation(
        summary = "Actualizar tipo de entidad",
        description = "Actualizamos un tipo de entidad existente en el sistema",
        tags = {"Tipos de Entidad"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El ID es requerido para realizar la actualización de un solo tipo de entidad",
                example = "1",
                required = true
            )
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos requeridos: 'tipo_rol'",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TipoEntidad.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado del tipo entidad mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = TipoEntidad.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Error en el formato del dato."
            ),
            @ApiResponse(
                responseCode = "404",
                description = "No existe el tipo de entidad"
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Sin permisos para realizar la petición."
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Error interno del servidor."
            ),
        }

    )
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
    @Operation(
        summary = "Eliminar tipo de entidad",
        description = "Eliminamos un tipo de entidad que exista en el sistema",
        tags = {"Tipos de Entidad"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El ID es requerido para realizar la eliminación de un solo tipo de entidad",
                example = "1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Dato eliminado sin problemas."
            ),
            @ApiResponse(
                responseCode = "404",
                description = "No existe el tipo de entidad"
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Sin permisos para realizar la petición."
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Error interno del servidor."
            ),
        }

    )
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
