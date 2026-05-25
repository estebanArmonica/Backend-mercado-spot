package com.safiraenergia.mercadospot.etl.loader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.etl.loader.LoadResult;
import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.repository.IEntidadRepository;
import com.safiraenergia.mercadospot.repository.IEstadoRepository;
import com.safiraenergia.mercadospot.repository.IFacturaRepository;
import com.safiraenergia.mercadospot.repository.IGlosaRepository;
import com.safiraenergia.mercadospot.repository.IPeriodoRepository;
import com.safiraenergia.mercadospot.repository.ITipoEntidadRepository;

@Slf4j
@Service
public class ETLTransactionService {
    @Autowired
    private IPeriodoRepository periodoRepo;
    
    @Autowired
    private IGlosaRepository glosaRepo;
    
    @Autowired
    private IEntidadRepository entidadRepo;
    
    @Autowired
    private IFacturaRepository facturaRepo;
    
    @Autowired
    private IEstadoRepository estadoRepo;
    
    @Autowired
    private ITipoEntidadRepository tipoEntidadRepo;

    private final ConcurrentHashMap<String, Periodo> periodoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Glosa> glosaCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entidad> entidadCache = new ConcurrentHashMap<>();

    @Transactional
    public LoadResult processBatch(List<FacturaDTO> facturas) {
        int inserted = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        for (FacturaDTO dto : facturas) {
            try {
                if (!validateDto(dto)) {
                    skipped++;
                    errors.add("Invalid DTO data for folio: " + dto.getFolio());
                    continue;
                }
                
                // Calcular monto total si es necesario
                if (dto.getMontoTotal() == 0 || dto.getMontoTotal() == 0) {
                    double calculatedTotal = dto.getMontoNeto() * 1.19;
                    dto.setMontoTotal((int) Math.round(calculatedTotal));
                }
                
                // Obtener o crear periodo
                Periodo periodo = getOrCreatePeriodo(dto.getPeriodo());
                int folioInt = dto.getFolio().intValue();
                Long periodoId = periodo.getId();
                
                // Verificar si ya existe
                long count = facturaRepo.countByFolioAndPeriodo(folioInt, periodoId);
                if (count > 0) {
                    log.debug("Factura with folio {} already exists", dto.getFolio());
                    skipped++;
                    continue;
                }
                
                // Obtener o crear estado
                String estadoNombre = dto.getEstado() != null ? dto.getEstado() : "PENDIENTE";
                Estado estado = estadoRepo.findByDescripcion(estadoNombre)
                    .orElseGet(() -> estadoRepo.save(Estado.builder().descripcion(estadoNombre).build()));
                
                // Obtener o crear tipo de entidad
                String tipoNombre = dto.getTipoEntidad() != null ? dto.getTipoEntidad() : "DEUDOR";
                TipoEntidad tipoEntidad = tipoEntidadRepo.findByTipoRol(tipoNombre)
                    .orElseGet(() -> tipoEntidadRepo.save(TipoEntidad.builder().tipoRol(tipoNombre).build()));
                
                // Obtener o crear entidad
                Entidad entidad = getOrCreateEntidad(dto.getRutEntidad(), dto.getNomEntidad(), tipoEntidad);
                
                // Obtener o crear glosa
                Glosa glosa = getOrCreateGlosa(dto.getGlosa());
                
                // Crear factura
                Factura factura = Factura.builder()
                    .folio(folioInt)
                    .montoNeto(dto.getMontoNeto())
                    .montoBruto(dto.getMontoBruto() != 0 ? dto.getMontoBruto() : 0)
                    .montoTotal(dto.getMontoTotal())
                    .fechaEmision(dto.getFechaEmision())
                    .fechaPago(dto.getFechaPago())
                    .periodo(periodo)
                    .entidad(entidad)
                    .glosa(glosa)
                    .build();
                
                // Agregar estado a la factura
                factura.getEstados().add(estado);
                
                facturaRepo.save(factura);
                inserted++;
                
            } catch (Exception e) {
                log.error("Error loading factura: {}", dto, e);
                errors.add("Failed to load folio " + dto.getFolio() + ": " + e.getMessage());
            }
        }
        if (inserted > 0) {
            facturaRepo.flush();
        }
        
        return new LoadResult(inserted, 0, skipped, errors);
    }

    private boolean validateDto(FacturaDTO dto) {
        if (dto == null) return false;
        if (dto.getFolio() == null || dto.getFolio() <= 0) return false;
        if (dto.getPeriodo() == null) return false;
        if (dto.getGlosa() == null || dto.getGlosa().trim().isEmpty()) return false;
        if (dto.getRutEntidad() == null || dto.getRutEntidad().trim().isEmpty()) return false;
        if (dto.getNomEntidad() == null || dto.getNomEntidad().trim().isEmpty()) return false;
        if (dto.getMontoNeto() == 0 || dto.getMontoNeto() < 0) return false;
        return true;
    }

    private Periodo getOrCreatePeriodo(Date mes) {
        if (mes == null) throw new IllegalArgumentException("Periodo date cannot be null");
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(mes);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        String key = year + "-" + month;
        
        return periodoCache.computeIfAbsent(key, k -> {
            return periodoRepo.findByYearAndMonth(year, month)
                .orElseGet(() -> periodoRepo.save(Periodo.builder().mes(mes).build()));
        });
    }
    
    private Glosa getOrCreateGlosa(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("Glosa description cannot be null");
        }
        
        String normalizedDesc = descripcion.trim().toUpperCase();
        
        return glosaCache.computeIfAbsent(normalizedDesc, d -> {
            return glosaRepo.findByDescripcion(d)
                .orElseGet(() -> glosaRepo.save(Glosa.builder().descripcion(d).build()));
        });
    }
    
    private Entidad getOrCreateEntidad(String rut, String nombre, TipoEntidad tipoEntidad) {
        if (rut == null || rut.trim().isEmpty()) {
            throw new IllegalArgumentException("RUT cannot be null");
        }
        
        String normalizedRut = rut.trim().toUpperCase();
        String normalizedNombre = nombre != null ? nombre.trim() : "";
        
        return entidadCache.computeIfAbsent(normalizedRut, r -> {
            Entidad entidad = entidadRepo.findByRutEntidad(r)
                .orElseGet(() -> {
                    Entidad nuevaEntidad = Entidad.builder()
                        .rutEntidad(normalizedRut)
                        .nombre(normalizedNombre)
                        .build();
                    return entidadRepo.save(nuevaEntidad);
                });
            
            // Agregar el tipo de entidad si no lo tiene
            if (tipoEntidad != null && !entidad.getTipoEntidad().contains(tipoEntidad)) {
                entidad.getTipoEntidad().add(tipoEntidad);
                entidad = entidadRepo.save(entidad);
                log.debug("Added tipoEntidad {} to entidad {}", tipoEntidad.getTipoRol(), normalizedRut);
            }
            
            return entidad;
        });
    }
}
