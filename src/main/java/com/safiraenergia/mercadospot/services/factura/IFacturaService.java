package com.safiraenergia.mercadospot.services.factura;

import com.safiraenergia.mercadospot.dto.FacturaDTO;
import com.safiraenergia.mercadospot.dto.FacturaFilterDTO;
import com.safiraenergia.mercadospot.dto.FacturaResponseDTO;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interfaz específica para operaciones de factura
 * Aplicando Principio de Segregación de Interfaces (ISP)
*/
public interface IFacturaService extends IGenericServiceUtils<Factura, Long>{
    FacturaResponseDTO createFactura(FacturaDTO facturaDTO);
    FacturaResponseDTO updateFactura(Long id, FacturaDTO facturaDTO);
    FacturaResponseDTO getFacturaById(Long id);
    Page<FacturaResponseDTO> getAllFacturas(Pageable pageable);
    Page<FacturaResponseDTO> filterFacturas(FacturaFilterDTO filter, Pageable pageable);
    List<FacturaResponseDTO> getFacturasByEntidad(String rutEntidad);
    List<FacturaResponseDTO> getFacturasByPeriodo(int year, int month);
    FacturaResponseDTO updateEstadoFactura(Long id, String estado);
    boolean existsByFolioAndPeriodo(Long folio, Long periodoId);
    Map<String, Object> getEstadisticasFacturas(LocalDate fechaInicio, LocalDate fechaFin);
    void deleteFactura(Long id);
}
