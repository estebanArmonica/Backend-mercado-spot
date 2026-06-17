package com.safiraenergia.mercadospot.controller;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.periodo.PeriodoDTO;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.services.periodo.IPeriodoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/periodo")
@Tag(name = "Periodos", description = "Endpoints para gestión de periodos")
public class PeriodoController {
    
    private final IPeriodoService periodoService;

    @Autowired
    public PeriodoController(IPeriodoService periodoService){
        this.periodoService = periodoService;
    }

    @GetMapping("list-all")
    @Operation(
        summary = "Obtener todos los periodos",
        description = "Retorna un conjunto de todos los periodos disponibles.",
        tags = {"Periodos"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de los periodos mostrado sin problemas",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Periodo.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay periodos disponibles para mostrar."
            )
        }
    )
    public ResponseEntity<List<PeriodoDTO>> getAllPeriodos() {
        log.info("Obteniendo todos los periodos");
        List<Periodo> periodos = periodoService.getAllPeriodos();
        return ResponseEntity.ok(periodos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/list-periodo/{id}")
    @Operation(
        summary = "Obtener periodo por ID",
        description = "Retorna un dato especifico encontrado por el ID",
        tags = {"Periodos"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El ID es requerido para realizar la busqueda",
                example = "1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Busqueda por ID del periodo especifico mostrado sin problemas",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Periodo.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No existe ese ID para buscar el dato."
            )
        }
    )
    public ResponseEntity<PeriodoDTO> getPeriodoById(@PathVariable Long id) {
        log.info("Obteniendo periodo con ID: {}", id);
        Periodo periodo = periodoService.getPeriodoById(id);
        return ResponseEntity.ok(convertToDTO(periodo));
    }

    @GetMapping("/year/{year}")
    @Operation(
        summary = "Obtener periodos por año",
        description = "Retornamos una lista de periodos por el año buscado",
        tags = {"Periodos"},
        parameters = {
            @Parameter(
                name = "year",
                description = "El year es requerido para realizar la busqueda",
                example = "2024",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Busqueda por years del periodo especifico mostrado sin problemas",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Periodo.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No existe ese año para buscar el dato."
            )
        }
    )
    public ResponseEntity<List<PeriodoDTO>> getPeriodosByYear(@PathVariable int year) {
        log.info("Obteniendo periodos por año: {}", year);
        List<Periodo> periodos = periodoService.getPeriodosByYear(year);
        return ResponseEntity.ok(periodos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/current")
    @Operation(
        summary = "Obtener periodo actual",
        description = "Retornamos el periodo actual correspondiente",
        tags = {"Periodos"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Mostrando el periodo actual sin problemas",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Periodo.class)
                )
            )
        }
    )
    public ResponseEntity<PeriodoDTO> getCurrentPeriodo() {
        log.info("Obteniendo periodo actual");
        Periodo periodo = periodoService.getCurrentPeriodo();
        return ResponseEntity.ok(convertToDTO(periodo));
    }

    @PostMapping("/create-new-periodo")
    @Operation(
        summary = "Crear nuevo periodo",
        description = "Retorna un objeto de creación de periodo",
        tags = {"Periodos"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos requeridos: 'mes'",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Periodo.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Creación de un nuevo periodo ingresado con exito"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Error de validación (e.g. campo mal ingresado)."
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Conflict (e.g. el mes ya está creado o ya existe en el sistema)."
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Error interno del servidor."
            ),
        }
    )
    public ResponseEntity<PeriodoDTO> createPeriodo(@RequestBody PeriodoDTO periodoDTO) {
        log.info("Creando periodo para fecha: {}", periodoDTO.getMes());
        
        Periodo periodo = Periodo.builder()
            .mes(periodoDTO.getMes())
            .build();
        
        Periodo saved = periodoService.createPeriodo(periodo);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar periodo",
        description = "Retorna el mensaje de que el periodo fue borrado con exito",
        tags = {"Periodos"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El id es requerido para realizar la busqueda",
                example = "1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Eliminación del dato de periodo fue mostrado con exito",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Periodo.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "No existe ese ID para buscar el dato."
            )
        }
    )
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
