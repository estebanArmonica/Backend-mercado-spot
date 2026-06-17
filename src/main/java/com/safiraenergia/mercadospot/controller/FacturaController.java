package com.safiraenergia.mercadospot.controller;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaFilterDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaResponseDTO;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.services.factura.IFacturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/factura")
@Tag(name = "Factura", description = "Endpoints para gestión de Facturas")
public class FacturaController {

    private final IFacturaService facturaService;

    @Autowired
    public FacturaController(IFacturaService facturaService){
        this.facturaService = facturaService;
    }
    
    @GetMapping("/list-all")
    @Operation(
        summary = "Obtener todas las facturas (paginado)",
        description = "Obtenemos todas las facturas de forma paginada en roden desc",
        tags = {"Factura"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Factura.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<PagedModel<FacturaResponseDTO>> getAllFacturas(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Obteniendo todas las facturas - página: {}, tamaño: {}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<FacturaResponseDTO> page = facturaService.getAllFacturas(pageable);
        PagedModel<FacturaResponseDTO> pagedModel = new PagedModel<>(page);

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/list-factura/{id}")
    @Operation(
        summary = "Obtener factura por ID",
        description = "Obtenemos una factura especifica por el ID",
        tags = {"Factura"},
        parameters = {
            @Parameter(
                name = "id",
                description = "El campo ID es requerido para realizar la busqueda de una factura",
                example = "1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Factura.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<FacturaResponseDTO> getFacturaById(@PathVariable Long id) {
        log.info("Obteniendo factura con ID: {}", id);

        return ResponseEntity.ok(facturaService.getFacturaById(id));
    }

    @GetMapping("/entidad/{rut}")
    @Operation(
        summary = "Obtener facturas por RUT de entidad",
        description = "Obtenemos una lista de las facturas a travez de su rut asociado",
        tags = {"Factura"},
        parameters = {
            @Parameter(
                name = "rut",
                description = "El campo rut es requerido para realizar la busqueda de facturas",
                example = "11.111.111-1",
                required = true
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Factura.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByEntidad(@PathVariable String rut) {
        log.info("Obteniendo facturas por entidad RUT: {}", rut);
        return ResponseEntity.ok(facturaService.getFacturasByEntidad(rut));
    }

    @GetMapping("/periodo")
    @Operation(
        summary = "Obtener facturas por año y mes",
        description = "Obtenemos una lista de las facturas por año y mes",
        tags = {"Factura"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(type = "array", implementation = Factura.class)
                )
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByPeriodo(
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Obteniendo facturas por periodo: {}-{}", year, month);
        return ResponseEntity.ok(facturaService.getFacturasByPeriodo(year, month));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Buscar facturas con filtros",
        description = "Realizamos una busqueda de una factura a travéz de diferentes filtros",
        tags = {"Factura"},
        parameters = {
            @Parameter(
                name = "folio",
                description = "El campo folio es requerido para realizar la busqueda de facturas",
                example = "1001",
                required = false
            ),
            @Parameter(
                name = "rutEntidad",
                description = "El campo rut es requerido para realizar la busqueda de facturas",
                example = "11.111.111-1",
                required = false
            ),
            @Parameter(
                name = "year",
                description = "El campo año es requerido para realizar la busqueda de facturas",
                example = "2024",
                required = false
            ),
            @Parameter(
                name = "month",
                description = "El campo mes es requerido para realizar la busqueda de facturas",
                example = "01",
                required = false
            ),
            @Parameter(
                name = "fechaDesde",
                description = "El campo fecha_desde es requerido para realizar la busqueda de facturas",
                example = "2024/01-01",
                required = false
            ),
            @Parameter(
                name = "fechaHasta",
                description = "El campo fecha_hasta es requerido para realizar la busqueda de facturas",
                example = "2026/01-01",
                required = false
            ),
            @Parameter(
                name = "montoMin",
                description = "El campo monto_min es requerido para realizar la busqueda de facturas",
                example = "5",
                required = false
            ),
            @Parameter(
                name = "montoMax",
                description = "El campo monto_max es requerido para realizar la busqueda de facturas",
                example = "30",
                required = false
            ),
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso."
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<PagedModel<FacturaResponseDTO>> searchFacturas(
            @RequestParam(required = false) Long folio,
            @RequestParam(required = false) String rutEntidad,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) Double montoMin,
            @RequestParam(required = false) Double montoMax,
            @PageableDefault(size = 20) Pageable pageable) {
        
        FacturaFilterDTO filter = FacturaFilterDTO.builder()
            .folio(folio)
            .rutEntidad(rutEntidad)
            .year(year)
            .month(month)
            .fechaEmisionDesde(fechaDesde)
            .fechaEmisionHasta(fechaHasta)
            .montoMinimo(montoMin)
            .montoMaximo(montoMax)
            .build();
        
        log.info("Buscando facturas con filtros: {}", filter);

        Page<FacturaResponseDTO> page = facturaService.filterFacturas(filter, pageable);
        PagedModel<FacturaResponseDTO> pagedModel = new PagedModel<>(page);

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/estadisticas")
    @Operation(
        summary = "Obtener estadísticas de facturas",
        description = "Obtenemos una estadistica de las facturas por fecha de inicio y fin",
        tags = {"Factura"},
        parameters = {
            @Parameter(
                name = "fechaInicio",
                description = "El campo fecha_inicio es requerido para realizar la busqueda de facturas",
                example = "2024/01-01",
                required = false
            ),
            @Parameter(
                name = "fechaFin",
                description = "El campo fecha_fin es requerido para realizar la busqueda de facturas",
                example = "2026/01-01",
                required = false
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Listado de las facturas mostrado exitoso."
            ),
            @ApiResponse(
                responseCode = "204",
                description = "No hay facturas para mostrar."
            )
        }
    )
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaFin) {
        
        log.info("Obteniendo estadísticas de facturas");
        Map<String, Object> estadisticas = facturaService.getEstadisticasFacturas(
            fechaInicio != null ? fechaInicio.toLocalDate() : null,
            fechaFin != null ? fechaFin.toLocalDate() : null
        );
        return ResponseEntity.ok(estadisticas);
    }

    @PostMapping("/created-factura")
    public ResponseEntity<FacturaResponseDTO> createFactura(@Valid @RequestBody FacturaDTO facturaDTO) {
        log.info("Creando factura con folio: {}", facturaDTO.getFolio());
        FacturaResponseDTO created = facturaService.createFactura(facturaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/update-factura/{id}")
    public ResponseEntity<FacturaResponseDTO> updateFactura(
            @PathVariable Long id,
            @Valid @RequestBody FacturaDTO facturaDTO) {
        log.info("Actualizando factura con ID: {}", id);
        return ResponseEntity.ok(facturaService.updateFactura(id, facturaDTO));
    }

    @PatchMapping("/updated-patch/{id}/estado")
    public ResponseEntity<FacturaResponseDTO> updateEstadoFactura(
            @PathVariable Long id,
            @RequestParam String estado) {
        log.info("Actualizando estado de factura {} a: {}", id, estado);
        return ResponseEntity.ok(facturaService.updateEstadoFactura(id, estado));
    }

    @DeleteMapping("/delete-factura/{id}")
    public ResponseEntity<Void> deleteFactura(@PathVariable Long id) {
        log.info("Eliminando factura con ID: {}", id);
        facturaService.deleteFactura(id);
        return ResponseEntity.noContent().build();
    }
}
