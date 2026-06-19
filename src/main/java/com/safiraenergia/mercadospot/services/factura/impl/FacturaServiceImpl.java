package com.safiraenergia.mercadospot.services.factura.impl;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaFilterDTO;
import com.safiraenergia.mercadospot.dto.factura.FacturaResponseDTO;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.repository.IFacturaRepository;
import com.safiraenergia.mercadospot.repository.IPeriodoRepository;
import com.safiraenergia.mercadospot.repository.IGlosaRepository;
import com.safiraenergia.mercadospot.repository.IEntidadRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.factura.IFacturaService;
import com.safiraenergia.mercadospot.specification.FacturaSpecification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de factura
 * Aplicando Patrón DTO, Patrón Mapper y Principio de Responsabilidad Única (SRP)
*/
@Slf4j
@Service
@Transactional
public class FacturaServiceImpl extends GenericServiceImpl<Factura, Long, IFacturaRepository> implements IFacturaService{
    
    private final IEntidadRepository entidadRepository;
    private final IPeriodoRepository periodoRepository;
    private final IGlosaRepository glosaRepository;

    @Autowired
    public FacturaServiceImpl(IFacturaRepository repository, IEntidadRepository entidadRepository, IPeriodoRepository periodoRepository, IGlosaRepository glosaRepository) {
        super(repository);
        this.entidadRepository = entidadRepository;
        this.periodoRepository = periodoRepository;
        this.glosaRepository = glosaRepository;
    }

    @Override
    @Transactional
    public FacturaResponseDTO createFactura(FacturaDTO facturaDTO) {
        log.debug("Creating new factura with folio: {}", facturaDTO.getFolio());
        
        // Validar que no exista
        if (existsByFolioAndPeriodo(facturaDTO.getFolio(), facturaDTO.getPeriodo().getTime())) {
            throw new RuntimeException("Factura already exists for this folio and period");
        }
        
        Factura factura = convertToEntity(facturaDTO);
        Factura saved = save(factura);
        
        log.info("Factura created successfully with id: {}", saved.getId());
        return convertToResponseDTO(saved);
    }

    @Override
    @Transactional
    public FacturaResponseDTO updateFactura(Long id, FacturaDTO facturaDTO) {
        log.debug("Updating factura with id: {}", id);
        
        Factura existing = findByIdOrThrow(id);
        updateEntity(existing, facturaDTO);
        Factura updated = update(existing);
        
        log.info("Factura updated successfully with id: {}", id);
        return convertToResponseDTO(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public FacturaResponseDTO getFacturaById(Long id) {
        log.debug("Getting factura by id: {}", id);
        Factura factura = findByIdOrThrow(id);
        return convertToResponseDTO(factura);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacturaResponseDTO> getAllFacturas(Pageable pageable) {
        log.debug("Getting all facturas with pagination and sorting: {} ", pageable);
        return repository.findAll(pageable).map(this::convertToResponseDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<FacturaResponseDTO> filterFacturas(FacturaFilterDTO filter, Pageable pageable) {
        log.debug("Filtering facturas with criteria: {}", filter);
        return repository.findAll(FacturaSpecification.withFilters(filter), pageable)
            .map(this::convertToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> getFacturasByEntidad(String rutEntidad) {
        log.debug("Getting facturas by entidad RUT: {}", rutEntidad);
        return repository.findByEntidadRutEntidad(rutEntidad).stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> getFacturasByPeriodo(int year, int month) {
        log.debug("Getting facturas by periodo: {}-{}", year, month);

        List<Factura> facturas = repository.findByPeriodoYearAndPeriodoMonth(year, month);

        return facturas.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public FacturaResponseDTO updateEstadoFactura(Long id, String estado) {
        log.debug("Updating estado for factura id: {} to {}", id, estado);
        
        Factura factura = findByIdOrThrow(id);
        // Actualizar estado según lógica de negocio
        // factura.setEstado(estado);
        
        Factura updated = update(factura);
        return convertToResponseDTO(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByFolioAndPeriodo(Long folio, Long periodoId) {
        return repository.countByFolioAndPeriodo(folio.intValue(), periodoId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEstadisticasFacturas(LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Getting estadisticas for period: {} to {}", fechaInicio, fechaFin);
        
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Implementar estadísticas según necesidades
        estadisticas.put("totalFacturas", repository.count());
        estadisticas.put("montoTotal", repository.sumMontoTotal());
        estadisticas.put("montoPromedio", repository.avgMontoTotal());
        
        return estadisticas;
    }
    
    @Override
    @Transactional
    public void deleteFactura(Long id) {
        log.debug("Deleting factura with id: {}", id);
        deleteById(id);
        log.info("Factura deleted successfully with id: {}", id);
    }
    
    // Implementación de métodos abstractos de GenericServiceImpl
    
    @Override
    protected Long getId(Factura entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(Factura entity) {
        if (entity.getFolio() <= 0) {
            throw new IllegalArgumentException("Folio must be greater than 0");
        }
        if (entity.getMontoNeto() < 0) {
            throw new IllegalArgumentException("Monto neto cannot be negative");
        }
        if (entity.getEntidad() == null) {
            throw new IllegalArgumentException("Entidad cannot be null");
        }
        if (entity.getPeriodo() == null) {
            throw new IllegalArgumentException("Periodo cannot be null");
        }
    }

    @Override
    protected void validateBeforeUpdate(Factura entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("Factura not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(Factura entity, Map<String, Object> updates) {
        // Implementar actualizaciones parciales
        updates.forEach((key, value) -> {
            switch (key) {
                case "montoNeto" -> entity.setMontoNeto((Integer) value);
                case "montoBruto" -> entity.setMontoBruto((Integer) value);
                case "montoTotal" -> entity.setMontoTotal((Integer) value);
                case "fechaPago" -> entity.setFechaPago((java.sql.Date) value);
                default -> log.warn("Unknown field for partial update: {}", key);
            }
        });
    }
    
    // Métodos de conversión (Mapper Pattern)
    
    private Factura convertToEntity(FacturaDTO dto) {
        Entidad entidad = entidadRepository.findByRutEntidad(dto.getRutEntidad())
            .orElseThrow(() -> new RuntimeException("Entidad not found with RUT: " + dto.getRutEntidad()));
        
        Periodo periodo = periodoRepository.findByMes(dto.getPeriodo())
            .orElseThrow(() -> new RuntimeException("Periodo not found for date: " + dto.getPeriodo()));
        
        Glosa glosa = glosaRepository.findByDescripcion(dto.getGlosa())
            .orElseThrow(() -> new RuntimeException("Glosa not found: " + dto.getGlosa()));
        
        return Factura.builder()
            .folio(dto.getFolio().intValue())
            .montoNeto(dto.getMontoNeto())
            .montoBruto(dto.getMontoBruto())
            .montoTotal(dto.getMontoTotal())
            .fechaEmision(dto.getFechaEmision())
            .fechaPago(dto.getFechaPago())
            .entidad(entidad)
            .periodo(periodo)
            .glosa(glosa)
            .build();
    }

    // método auxiliar para transformar date a string
    private String formatDate(java.sql.Date date) {
        if (date == null) return null;

        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
    
    private FacturaResponseDTO convertToResponseDTO(Factura factura) {

        // Obtenemos el estado principal (el primer estado de la lista)
        String estadoPrincipal = null;
        Set<String> estadosNombres = null;

        if(factura.getEstados() != null && !factura.getEstados().isEmpty()){
            // en caso de querer todos los estados como string realizamos esta parte
            estadosNombres = factura.getEstados().stream()
                .map(Estado::getDescripcion)
                .collect(Collectors.toSet()
            );

            // Tomamos el primer estado como principal 
            estadoPrincipal = factura.getEstados().iterator().next().getDescripcion();
        }

        // convertimos e Date de sql a LocalDate
        return FacturaResponseDTO.builder()
            .id(factura.getId())
            .folio((long) factura.getFolio())
            .montoNeto(factura.getMontoNeto())
            .montoBruto(factura.getMontoBruto())
            .montoTotal(factura.getMontoTotal())
            .fechaEmision(factura.getFechaEmision())
            .fechaPago(factura.getFechaPago())
            .rutEntidad(factura.getEntidad().getRutEntidad())
            .nombreEntidad(factura.getEntidad().getNombre())
            .glosa(factura.getGlosa().getDescripcion())
            .periodo(formatDate(factura.getPeriodo().getMes()))
            .estado(estadoPrincipal)
            .estados(estadosNombres)
            .build();
    }
    
    private void updateEntity(Factura existing, FacturaDTO dto) {
        if (dto.getMontoNeto() !=  0) existing.setMontoNeto(dto.getMontoNeto());
        if (dto.getMontoBruto() != 0) existing.setMontoBruto(dto.getMontoBruto());
        if (dto.getMontoTotal() != 0) existing.setMontoTotal(dto.getMontoTotal());
        if (dto.getFechaPago() !=  null) existing.setFechaPago(dto.getFechaPago());
    }

    // Métodos que aún no se usaran
    @Override
    public void softDelete(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'softDelete'");
    }

    @Override
    public void restore(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'restore'");
    }

    @Override
    public List<Factura> findActive() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findActive'");
    }

    @Override
    public List<Factura> findInactive() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findInactive'");
    }
}
