package com.safiraenergia.mercadospot.etl.loader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    // 🔥 Limitar tamaño de cachés
    private final ConcurrentHashMap<String, Periodo> periodoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Glosa> glosaCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entidad> entidadCache = new ConcurrentHashMap<>();
    
    // 🔥 Contador para limpiar caché periódicamente
    private int processedCount = 0;
    private static final int CACHE_CLEANUP_THRESHOLD = 500;

    @Transactional(rollbackFor = Exception.class, timeout = 120) // 🔥 Reducido de 300 a 120 segundos
    public LoadResult processBatch(List<FacturaDTO> facturas) {
        int inserted = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        // 🔥 Obtener o crear estado y tipo entidad una sola vez por batch
        Estado estadoPendiente = estadoRepo.findByDescripcion("PENDIENTE")
            .orElseGet(() -> estadoRepo.save(Estado.builder().descripcion("PENDIENTE").build()));
        TipoEntidad tipoDeudor = tipoEntidadRepo.findByTipoRol("DEUDOR")
            .orElseGet(() -> tipoEntidadRepo.save(TipoEntidad.builder().tipoRol("DEUDOR").build()));
        
        for (FacturaDTO dto : facturas) {
            try {
                // 🔥 Validación rápida
                if (!validateDto(dto)) {
                    skipped++;
                    errors.add("Invalid DTO data for folio: " + dto.getFolio());
                    continue;
                }
                
                // 🔥 Calcular monto total si es necesario
                if (dto.getMontoTotal() == 0) {
                    double calculatedTotal = dto.getMontoNeto() * 1.19;
                    dto.setMontoTotal((int) Math.round(calculatedTotal));
                }
                
                // 🔥 Obtener periodo (usando caché)
                Periodo periodo = getOrCreatePeriodo(dto.getPeriodo());
                int folioInt = dto.getFolio().intValue();
                
                // 🔥 Verificar existencia con query optimizada
                if (facturaRepo.existsByFolioAndPeriodoId(folioInt, periodo.getId())) {
                    log.debug("Factura with folio {} already exists", dto.getFolio());
                    skipped++;
                    continue;
                }
                
                // 🔥 Obtener entidad (usando caché)
                Entidad entidad = getOrCreateEntidad(dto.getRutEntidad(), dto.getNomEntidad(), tipoDeudor);
                
                // 🔥 Obtener glosa (usando caché)
                Glosa glosa = getOrCreateGlosa(dto.getGlosa());
                
                // 🔥 Crear factura SIN saveAndFlush()
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
                
                factura.getEstados().add(estadoPendiente);
                
                // 🔥 Guardar usando persist (más eficiente que saveAndFlush)
                entityManager.persist(factura);
                inserted++;
                processedCount++;
                
                // 🔥 Limpiar caché periódicamente para evitar memory leak
                if (processedCount >= CACHE_CLEANUP_THRESHOLD) {
                    cleanupCaches();
                    processedCount = 0;
                }
                
            } catch (Exception e) {
                log.error("Error loading factura with folio: {}", dto.getFolio(), e);
                errors.add("Failed to load folio " + dto.getFolio() + ": " + e.getMessage());
                skipped++;
            }
        }
        
        // 🔥 Flush al final del batch
        entityManager.flush();
        
        return new LoadResult(inserted, 0, skipped, errors);
    }

    private boolean validateDto(FacturaDTO dto) {
        if (dto == null) return false;
        if (dto.getFolio() == null || dto.getFolio() <= 0) return false;
        if (dto.getPeriodo() == null) return false;
        if (dto.getGlosa() == null || dto.getGlosa().trim().isEmpty()) return false;
        if (dto.getRutEntidad() == null || dto.getRutEntidad().trim().isEmpty()) return false;
        if (dto.getNomEntidad() == null || dto.getNomEntidad().trim().isEmpty()) return false;
        if (dto.getMontoNeto() <= 0) {
            log.debug("Invalid montoNeto: {}", dto.getMontoNeto());
            return false;
        }
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
                    // 🔥 Usar persist en lugar de saveAndFlush
                    entityManager.persist(nuevaEntidad);
                    return nuevaEntidad;
                });
            
            // Agregar el tipo de entidad si no lo tiene
            if (tipoEntidad != null && !entidad.getTipoEntidad().contains(tipoEntidad)) {
                entidad.getTipoEntidad().add(tipoEntidad);
                // 🔥 Usar merge en lugar de saveAndFlush
                entidad = entityManager.merge(entidad);
                log.debug("Added tipoEntidad {} to entidad {}", tipoEntidad.getTipoRol(), normalizedRut);
            }
            
            return entidad;
        });
    }

    // 🔥 Método para limpiar cachés
    private void cleanupCaches() {
        log.debug("Limpiando cachés - Periodos: {}, Glosas: {}, Entidades: {}", 
            periodoCache.size(), glosaCache.size(), entidadCache.size());
        
        if (periodoCache.size() > 200) {
            periodoCache.clear();
        }
        if (glosaCache.size() > 500) {
            glosaCache.clear();
        }
        if (entidadCache.size() > 500) {
            entidadCache.clear();
        }
        
        // 🔥 Limpiar el EntityManager
        entityManager.flush();
        entityManager.clear();
    }
}