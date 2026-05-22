package com.safiraenergia.mercadospot.etl.loader;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.dto.factura.FacturaDTO;
import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.models.Factura;
import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.repository.IEntidadRepository;
import com.safiraenergia.mercadospot.repository.IFacturaRepository;
import com.safiraenergia.mercadospot.repository.IGlosaRepository;
import com.safiraenergia.mercadospot.repository.IPeriodoRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Component
public class DataLoader {
    
    @Autowired
    private IPeriodoRepository periodoRepo;
    
    @Autowired
    private IGlosaRepository glosaRepo;
    
    @Autowired
    private IEntidadRepository entidadRepo;
    
    @Autowired
    private IFacturaRepository facturaRepo;

    private final Map<String, Periodo> periodoCache = new ConcurrentHashMap<>();
    private final Map<String, Glosa> glosaCache = new ConcurrentHashMap<>();
    private final Map<String, Entidad> entidadCache = new ConcurrentHashMap<>();

    @Transactional
    public LoadResult load(List<FacturaDTO> facturas) {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        for (FacturaDTO dto : facturas) {
            try {
                // validamos el DTO antes de procesar
                if(!validateDto(dto)){
                    skipped++;
                    errors.add("Invalid DTO data for folio: " + dto.getFolio());
                    continue;
                }

                // verificamos si ya existe
                Periodo periodo = getOrCreatePeriodo(dto.getPeriodo());

                int folioInt = dto.getFolio().intValue();
                Long periodoId = periodo.getId();

                // verificamos si ya existe usando el método
                long count = facturaRepo.countByFolioAndPeriodo(folioInt, periodoId);

                if (count > 0) {
                    log.warn("Factura with folio {} already exists for period", dto.getFolio(), periodoId);
                    skipped++;
                    continue;
                }
                
                Factura factura = buildFactura(dto, periodo);
                facturaRepo.save(factura);
                inserted++;
                
                if (inserted % 50 == 0) {
                    facturaRepo.flush();
                    clearCacheIfNeeded();
                }
            } catch (Exception e) {
                log.error("Error loading factura: {}", dto, e);
                errors.add("Failed to load folio " + dto.getFolio() + ": " + e.getMessage());
            }
        }
        
        log.info("Load completed - Inserted: {}, Updated: {}, Skipped: {}, Errors: {}", 
                 inserted, updated, skipped, errors.size());
        
        return new LoadResult(inserted, updated, skipped, errors);
    }

    private boolean validateDto(FacturaDTO dto){
        if(dto == null) return false;
        if(dto.getFolio() == null || dto.getFolio() <= 0) return false;
        if(dto.getPeriodo() == null) return false;
        if(dto.getGlosa() == null || dto.getGlosa().trim().isEmpty()) return false;
        if(dto.getRutEntidad() == null || dto.getRutEntidad().trim().isEmpty()) return false;
        if(dto.getNomEntidad() == null || dto.getNomEntidad().trim().isEmpty()) return false;
        
        return true;
    }

    private Factura buildFactura(FacturaDTO dto, Periodo periodo) {
        Glosa glosa = getOrCreateGlosa(dto.getGlosa());
        Entidad entidad = getOrCreateEntidad(dto.getRutEntidad(), dto.getNomEntidad());
        
        return Factura.builder()
            .folio(dto.getFolio().intValue())
            .montoNeto(dto.getMontoNeto() != 0 ? dto.getMontoNeto() : 0)
            .montoBruto(dto.getMontoBruto() != 0 ? dto.getMontoBruto() : 0)
            .montoTotal(dto.getMontoTotal() != 0 ? dto.getMontoTotal() : 0)
            .fechaEmision(dto.getFechaEmision())
            .fechaPago(dto.getFechaPago())
            .periodo(periodo)
            .entidad(entidad)
            .glosa(glosa)
            .build();
    }

    private Periodo getOrCreatePeriodo(Date mes) {
        if(mes == null){
            throw new IllegalArgumentException("Periodo date cannot be null");
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(mes);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        String key = year + "-" + month;
        
        return periodoCache.computeIfAbsent(key, k -> {
            return periodoRepo.findByYearAndMonth(year, month)
                .orElseGet(() -> {
                    Periodo nuevoPer = Periodo.builder()
                        .mes(mes)
                        .build();
                    return periodoRepo.save(nuevoPer);
                });
        });
    }

    private Glosa getOrCreateGlosa(String descripcion) {
        if(descripcion == null || descripcion.trim().isEmpty()){
            throw new IllegalArgumentException("Glosa descripcion cannot be null or empty");
        }

        String normalizedDesc = descripcion.trim().toUpperCase();

        return glosaCache.computeIfAbsent(normalizedDesc, d -> {
            return glosaRepo.findByDescripcion(d)
                .orElseGet(() -> {
                    Glosa nuevaGlosa = Glosa.builder()
                        .descripcion(d)
                        .build();
                    return glosaRepo.save(nuevaGlosa);
                });
        });
    }
    
    private Entidad getOrCreateEntidad(String rut, String nombre) {
        if (rut == null || rut.trim().isEmpty()) {
            throw new IllegalArgumentException("RUT cannot be null or empty");
        }
        
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }

        String normalizedRut = rut.trim().toUpperCase();
        String normalizedNombre = nombre.trim();

        return entidadCache.computeIfAbsent(normalizedRut, r -> {
            return entidadRepo.findByRutEntidad(r)
                .orElseGet(() -> {
                    Entidad nuevaEntidad = Entidad.builder()
                        .rutEntidad(normalizedRut)
                        .nombre(normalizedNombre)
                        .build();
                    return entidadRepo.save(nuevaEntidad);
                });
        });
    }

    private void clearCacheIfNeeded() {
        // limpiamos cachés periódicamente para evitar consumo excesivo de memoria
        if(periodoCache.size() > 1000){
            periodoCache.clear();
            glosaCache.clear();
            entidadCache.clear();
            log.debug("Cleared caches to free memory");
        }
    }
}
