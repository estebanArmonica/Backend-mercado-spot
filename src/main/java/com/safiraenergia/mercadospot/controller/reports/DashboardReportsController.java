package com.safiraenergia.mercadospot.controller.reports;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safiraenergia.mercadospot.services.factura.IFacturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600, methods = RequestMethod.GET)
@Tag(name = "Dashboard", description = "Endpoints para reportes y estadísticas")
public class DashboardReportsController {
    
    private final IFacturaService facturaService;

    @Autowired
    public DashboardReportsController(IFacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen del dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardResumen() {
        log.info("Obteniendo resumen del dashboard");
        
        Map<String, Object> resumen = new HashMap<>();
        
        // Estadísticas generales
        Map<String, Object> estadisticas = facturaService.getEstadisticasFacturas(null, null);
        resumen.put("estadisticas", estadisticas);
        
        // Puedes agregar más métricas aquí
        // - Facturas por estado
        // - Facturas por periodo
        // - Top deudores/acreedores
        // - Montos por mes
        
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/facturas-por-mes")
    @Operation(summary = "Obtener facturas agrupadas por mes")
    public ResponseEntity<Map<String, Object>> getFacturasPorMes(@RequestParam int year) {
        log.info("Obteniendo facturas por mes para año: {}", year);
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("year", year);
        // Implementar agrupación por mes
        
        return ResponseEntity.ok(resultado);
    }
}
