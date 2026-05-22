package com.safiraenergia.mercadospot.controller;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaFilterDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaResponseDTO;
import com.safiraenergia.mercadospot.services.factura.IFacturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/factura")
@Tag(name = "Factura Controller", description = "Endpoints para gestión de Facturas")
@CrossOrigin(origins = "*", methods = {RequestMethod.DELETE, RequestMethod.GET, RequestMethod.PATCH, RequestMethod.POST, RequestMethod.PUT}, maxAge = 3600)
public class FacturaController {

    private final IFacturaService facturaService;

    @Autowired
    public FacturaController(IFacturaService facturaService){
        this.facturaService = facturaService;
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener todas las facturas (paginado)")
    public ResponseEntity<Page<FacturaResponseDTO>> getAllFacturas(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.info("Obteniendo todas las facturas - página: {}, tamaño: {}", pageable.getPageNumber(), pageable.getPageSize());

        return ResponseEntity.ok(facturaService.getAllFacturas(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener factura por ID")
    public ResponseEntity<FacturaResponseDTO> getFacturaById(@PathVariable Long id) {
        log.info("Obteniendo factura con ID: {}", id);

        return ResponseEntity.ok(facturaService.getFacturaById(id));
    }

    @GetMapping("/entidad/{rut}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener facturas por RUT de entidad")
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByEntidad(@PathVariable String rut) {
        log.info("Obteniendo facturas por entidad RUT: {}", rut);
        return ResponseEntity.ok(facturaService.getFacturasByEntidad(rut));
    }

    @GetMapping("/periodo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener facturas por año y mes")
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByPeriodo(
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Obteniendo facturas por periodo: {}-{}", year, month);
        return ResponseEntity.ok(facturaService.getFacturasByPeriodo(year, month));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar facturas con filtros")
    public ResponseEntity<Page<FacturaResponseDTO>> searchFacturas(
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
        return ResponseEntity.ok(facturaService.filterFacturas(filter, pageable));
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VIEWER')")
    @Operation(summary = "Obtener estadísticas de facturas")
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

    @PostMapping
    @Operation(summary = "Crear factura manualmente")
    public ResponseEntity<FacturaResponseDTO> createFactura(@Valid @RequestBody FacturaDTO facturaDTO) {
        log.info("Creando factura con folio: {}", facturaDTO.getFolio());
        FacturaResponseDTO created = facturaService.createFactura(facturaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar factura")
    public ResponseEntity<FacturaResponseDTO> updateFactura(
            @PathVariable Long id,
            @Valid @RequestBody FacturaDTO facturaDTO) {
        log.info("Actualizando factura con ID: {}", id);
        return ResponseEntity.ok(facturaService.updateFactura(id, facturaDTO));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado de factura")
    public ResponseEntity<FacturaResponseDTO> updateEstadoFactura(
            @PathVariable Long id,
            @RequestParam String estado) {
        log.info("Actualizando estado de factura {} a: {}", id, estado);
        return ResponseEntity.ok(facturaService.updateEstadoFactura(id, estado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar factura")
    public ResponseEntity<Void> deleteFactura(@PathVariable Long id) {
        log.info("Eliminando factura con ID: {}", id);
        facturaService.deleteFactura(id);
        return ResponseEntity.noContent().build();
    }
}
