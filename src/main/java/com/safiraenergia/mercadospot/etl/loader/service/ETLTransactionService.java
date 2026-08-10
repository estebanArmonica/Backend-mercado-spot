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

    // Creamos caches para cada tabla
    private final ConcurrentHashMap<String, Estado> estadoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TipoEntidad> tipoEntidadCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Periodo> periodoCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Glosa> glosaCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entidad> entidadCache = new ConcurrentHashMap<>();
    
    // Contador para limpiar caché periódicamente
    private int processedCount = 0;
    private static final int CACHE_CLEANUP_THRESHOLD = 500;

    @Transactional(rollbackFor = Exception.class, timeout = 120)
    public LoadResult processBatch(List<FacturaDTO> facturas) {
        int inserted = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        // Log de las primeras facturas para el debug
        if(!facturas.isEmpty()) {
            FacturaDTO first = facturas.get(0);
            log.info("Primera factura del batch - Folio: {}, Neto: {}, Periodo: {}, Glosa: {}, RUT: {}", 
                first.getFolio(), first.getMontoNeto(), first.getPeriodo(), 
                first.getGlosa(), first.getRutEntidad()
            );
        }
        
        // Obtener o crear estado y tipo entidad UNA SOLA VEZ
        Estado estadoPendiente = getOrCreateEstado("PENDIENTE");
        
        for (FacturaDTO dto : facturas) {
            try {
                if (!validateDto(dto)) {
                    skipped++;
                    errors.add("Invalid DTO data for folio: " + dto.getFolio());
                    continue;
                }

                // Obtenemos le tipo de entidad con DTO
                String tipoEntidadStr = dto.getTipoEntidad();
                if(tipoEntidadStr == null || tipoEntidadStr.trim().isEmpty()) {
                    tipoEntidadStr = "DEUDOR"; // Valor por defecto
                }

                // Obtenemos el tipo de entidad
                TipoEntidad tipoEntidad = getOrCreateTipoEntidad(tipoEntidadStr.toUpperCase());
                
                if (dto.getMontoTotal() == null || dto.getMontoTotal() == 0) {
                    Double montoNeto = dto.getMontoNeto();
                    if (montoNeto != null && montoNeto > 0) {
                        double calculatedTotal = dto.getMontoNeto() * 1.19;
                        double totalRedondeado = Math.round(calculatedTotal);
                        dto.setMontoTotal(totalRedondeado);
                    } else {
                        dto.setMontoTotal(0.0);
                    }
                }
                
                Periodo periodo = getOrCreatePeriodo(dto.getPeriodo());
                int folioInt = dto.getFolio().intValue();
                
                long count = facturaRepo.countByFolioAndPeriodo(folioInt, periodo.getId());
                if (count > 0) {
                    log.debug("Factura with folio {} already exists", dto.getFolio());
                    skipped++;
                    continue;
                }
                
                // 🔥 Obtener entidad (asegurar que existe)
                Entidad entidad = getOrCreateEntidad(dto.getRutEntidad(), dto.getNomEntidad(), tipoEntidad);
                Glosa glosa = getOrCreateGlosa(dto.getGlosa());
                
                // 🔥 Crear factura
                Factura factura = Factura.builder()
                    .folio(folioInt)
                    .montoNeto(dto.getMontoNeto() != null ? dto.getMontoNeto().intValue() : 0)
                    .montoBruto(dto.getMontoBruto() != null ? dto.getMontoBruto().intValue() : 0)
                    .montoTotal(dto.getMontoTotal() != null ? dto.getMontoTotal().intValue() : 0)
                    .fechaEmision(dto.getFechaEmision())
                    .fechaPago(dto.getFechaPago())
                    .periodo(periodo)
                    .entidad(entidad)
                    .glosa(glosa)
                    .build();
                
                // 🔥 Agregar estado (asegurar que está gestionado)
                factura.getEstados().add(estadoPendiente);
                
                // 🔥 Guardar usando persist
                entityManager.merge(factura);
                inserted++;
                processedCount++;
                
                if (processedCount >= CACHE_CLEANUP_THRESHOLD) {
                    cleanupCaches();
                    processedCount = 0;
                }
                
            } catch (Exception e) {
                log.error("Error loading factura with folio: {}", dto.getFolio(), e);
                errors.add("Failed to load folio " + dto.getFolio() + ": " + e.getMessage());
                skipped++;
                entityManager.clear();
            }
        }
        
        entityManager.flush();
        return new LoadResult(inserted, 0, skipped, errors);
    }

    // metodos privados
    private Estado getOrCreateEstado(String descripcion) {
        return estadoCache.computeIfAbsent(descripcion, d -> {
            return estadoRepo.findByDescripcion(d)
                .map(existing -> {
                    // 🔥 Si existe, asegurar que está gestionado
                    if (!entityManager.contains(existing)) {
                        return entityManager.merge(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Estado nuevo = Estado.builder().descripcion(d).build();
                    entityManager.persist(nuevo);
                    return nuevo;
                });
        });
    }

    private TipoEntidad getOrCreateTipoEntidad(String tipoRol) {
        return tipoEntidadCache.computeIfAbsent(tipoRol, t -> {
            return tipoEntidadRepo.findByTipoRol(t)
                .map(existing -> {
                    if (!entityManager.contains(existing)) {
                        return entityManager.merge(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    TipoEntidad nuevo = TipoEntidad.builder().tipoRol(t).build();
                    entityManager.persist(nuevo);
                    return nuevo;
                });
        });
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
                .map(existing -> {
                    if (!entityManager.contains(existing)) {
                        return entityManager.merge(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Periodo nuevo = Periodo.builder().mes(mes).build();
                    entityManager.persist(nuevo);
                    return nuevo;
                });
        });
    }
    
    private Glosa getOrCreateGlosa(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("Glosa description cannot be null");
        }
        
        String normalizedDesc = descripcion.trim().toUpperCase();
        
        return glosaCache.computeIfAbsent(normalizedDesc, d -> {
            return glosaRepo.findByDescripcion(d)
                .map(existing -> {
                    if (!entityManager.contains(existing)) {
                        return entityManager.merge(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Glosa nuevo = Glosa.builder().descripcion(d).build();
                    entityManager.persist(nuevo);
                    return nuevo;
                });
        });
    }
    
    private Entidad getOrCreateEntidad(String rut, String nombre, TipoEntidad tipoEntidad) {
        if (rut == null || rut.trim().isEmpty()) {
            throw new IllegalArgumentException("RUT cannot be null");
        }
        
        String normalizedRut = rut.trim().toUpperCase();
        String normalizedNombre = nombre != null ? nombre.trim() : "";
        
        return entidadCache.computeIfAbsent(normalizedRut, r -> {
            Entidad entidad = entidadRepo.findByRutEntidad(r).orElse(null);
            
            if (entidad == null) {
                entidad = Entidad.builder()
                    .rutEntidad(normalizedRut)
                    .nombre(normalizedNombre)
                    .build();
                entityManager.persist(entidad);
                log.debug("Created new entidad: {}", normalizedRut);
            } else {
                // 🔥 Asegurar que está gestionada
                if (!entityManager.contains(entidad)) {
                    entidad = entityManager.merge(entidad);
                }
            }
            
            // Agregamos el tipo de entidad 
            if(tipoEntidad != null) {
                // verificamos si la entidad ya tiene este tipo
                boolean hasTipo = entidad.getTipoEntidad().stream()
                    .anyMatch(t -> t.getTipoRol().equals(tipoEntidad.getTipoRol()));

                if(!hasTipo) {
                    // nos aseguramos que tipoEntidad está gestionada
                    TipoEntidad managedTipo = tipoEntidad;
                    if(!entityManager.contains(managedTipo)) {
                        managedTipo = entityManager.merge(tipoEntidad);
                    }
                    entidad.getTipoEntidad().add(managedTipo);
                    entidad = entityManager.merge(entidad);
                    log.debug("Added tipoEntidad {} to entidad {}", managedTipo.getTipoRol(), normalizedRut);
                }
            }
            
            return entidad;
        });
    }

    // 🔥 Método para limpiar cachés
    private void cleanupCaches() {
        log.debug("Limpiando cachés - Periodos: {}, Glosas: {}, Entidades: {}, Estados: {}, Tipos: {}", periodoCache.size(), glosaCache.size(), entidadCache.size(), estadoCache.size(), tipoEntidadCache.size());
        
        if (periodoCache.size() > 200) periodoCache.clear();
        if (glosaCache.size() > 500) glosaCache.clear();
        if (entidadCache.size() > 500) entidadCache.clear();
        if (estadoCache.size() > 100) estadoCache.clear();
        if (tipoEntidadCache.size() > 100) tipoEntidadCache.clear();
        
        // Limpiar el EntityManager
        entityManager.flush();
        entityManager.clear();
    }
}