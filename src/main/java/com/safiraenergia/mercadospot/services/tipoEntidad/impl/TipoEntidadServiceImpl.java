package com.safiraenergia.mercadospot.services.tipoEntidad.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.repository.ITipoEntidadRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.tipoEntidad.ITipoEntidadService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class TipoEntidadServiceImpl extends GenericServiceImpl<TipoEntidad, Long, ITipoEntidadRepository> implements ITipoEntidadService{
    
    @Autowired
    public TipoEntidadServiceImpl(ITipoEntidadRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public TipoEntidad createTipoEntidad(TipoEntidad tipoEntidad) {
        log.debug("Creating tipoEntidad with tipoRol: {}", tipoEntidad.getTipoRol());
        
        if (existsByTipoRol(tipoEntidad.getTipoRol())) {
            throw new RuntimeException("TipoEntidad already exists with tipoRol: " + tipoEntidad.getTipoRol());
        }
        
        TipoEntidad saved = save(tipoEntidad);
        log.info("TipoEntidad created successfully with id: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public TipoEntidad updateTipoEntidad(Long id, TipoEntidad tipoEntidad) {
        log.debug("Updating tipoEntidad with id: {}", id);
        
        TipoEntidad existing = findByIdOrThrow(id);
        
        if (tipoEntidad.getTipoRol() != null && !tipoEntidad.getTipoRol().equals(existing.getTipoRol())) {
            if (existsByTipoRol(tipoEntidad.getTipoRol())) {
                throw new RuntimeException("TipoRol already exists: " + tipoEntidad.getTipoRol());
            }
            existing.setTipoRol(tipoEntidad.getTipoRol());
        }
        
        TipoEntidad updated = update(existing);
        log.info("TipoEntidad updated successfully with id: {}", id);
        
        return updated;
    }
    
    @Override
    @Transactional(readOnly = true)
    public TipoEntidad getTipoEntidadById(Long id) {
        log.debug("Getting tipoEntidad by id: {}", id);
        return findByIdOrThrow(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TipoEntidad> getTipoEntidadByTipoRol(String tipoRol) {
        log.debug("Getting tipoEntidad by tipoRol: {}", tipoRol);
        return repository.findByTipoRol(tipoRol);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TipoEntidad> getAllTipoEntidades() {
        log.debug("Getting all tipoEntidades");
        return findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TipoEntidad> getTipoEntidadesByTipoRolContaining(String keyword) {
        log.debug("Getting tipoEntidades by tipoRol containing: {}", keyword);
        return repository.findByTipoRolContainingIgnoreCase(keyword);
    }
    
    @Override
    @Transactional
    public void deleteTipoEntidad(Long id) {
        log.debug("Deleting tipoEntidad with id: {}", id);
        deleteById(id);
        log.info("TipoEntidad deleted successfully with id: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByTipoRol(String tipoRol) {
        return repository.existsByTipoRol(tipoRol);
    }
    
    @Override
    @Transactional
    public TipoEntidad findOrCreateByTipoRol(String tipoRol) {
        log.debug("Finding or creating tipoEntidad with tipoRol: {}", tipoRol);
        
        String normalizedTipoRol = tipoRol.trim().toUpperCase();
        
        return getTipoEntidadByTipoRol(normalizedTipoRol)
            .orElseGet(() -> {
                TipoEntidad nuevaTipoEntidad = TipoEntidad.builder()
                    .tipoRol(normalizedTipoRol)
                    .build();
                return createTipoEntidad(nuevaTipoEntidad);
            });
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TipoEntidad> getTipoEntidadesOrdenadas() {
        log.debug("Getting all tipoEntidades ordered by tipoRol");
        return repository.findAllByOrderByTipoRolAsc();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TipoEntidad> getTipoEntidadCliente() {
        log.debug("Getting tipoEntidad CLIENTE");
        return getTipoEntidadByTipoRol("CLIENTE");
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TipoEntidad> getTipoEntidadProveedor() {
        log.debug("Getting tipoEntidad PROVEEDOR");
        return getTipoEntidadByTipoRol("PROVEEDOR");
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TipoEntidad> getTipoEntidadEmpleado() {
        log.debug("Getting tipoEntidad EMPLEADO");
        return getTipoEntidadByTipoRol("EMPLEADO");
    }
    
    // Implementación de métodos abstractos de GenericServiceImpl
    
    @Override
    protected Long getId(TipoEntidad entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(TipoEntidad entity) {
        if (entity.getTipoRol() == null || entity.getTipoRol().trim().isEmpty()) {
            throw new IllegalArgumentException("TipoRol cannot be null or empty");
        }
        if (entity.getTipoRol().length() > 20) {
            throw new IllegalArgumentException("TipoRol length must be less than 20 characters");
        }
    }
    
    @Override
    protected void validateBeforeUpdate(TipoEntidad entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("TipoEntidad not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(TipoEntidad entity, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "tipoRol" -> entity.setTipoRol((String) value);
                default -> log.warn("Unknown field for partial update: {}", key);
            }
        });
    }
    
    // Métodos no implementados de IGenericServiceUtils
    @Override
    public void softDelete(Long id) {
        throw new UnsupportedOperationException("Soft delete not implemented for TipoEntidad");
    }
    
    @Override
    public void restore(Long id) {
        throw new UnsupportedOperationException("Restore not implemented for TipoEntidad");
    }
    
    @Override
    public List<TipoEntidad> findActive() {
        return getAllTipoEntidades();
    }
    
    @Override
    public List<TipoEntidad> findInactive() {
        return List.of();
    }
}
