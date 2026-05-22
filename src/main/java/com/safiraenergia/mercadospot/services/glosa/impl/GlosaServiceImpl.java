package com.safiraenergia.mercadospot.services.glosa.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.repository.IGlosaRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.glosa.IGlosaService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class GlosaServiceImpl extends GenericServiceImpl<Glosa, Long, IGlosaRepository> implements IGlosaService {
    
    @Autowired
    public GlosaServiceImpl(IGlosaRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Glosa createGlosa(Glosa glosa) {
        log.debug("Creating glosa with descripcion: {}", glosa.getDescripcion());
        
        if (existsByDescripcion(glosa.getDescripcion())) {
            throw new RuntimeException("Glosa already exists with descripcion: " + glosa.getDescripcion());
        }
        
        Glosa saved = save(glosa);
        log.info("Glosa created successfully with id: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public Glosa updateGlosa(Long id, Glosa glosa) {
        log.debug("Updating glosa with id: {}", id);
        
        Glosa existing = findByIdOrThrow(id);
        
        if (glosa.getDescripcion() != null && !glosa.getDescripcion().equals(existing.getDescripcion())) {
            if (existsByDescripcion(glosa.getDescripcion())) {
                throw new RuntimeException("Descripcion already exists: " + glosa.getDescripcion());
            }
            existing.setDescripcion(glosa.getDescripcion());
        }
        
        Glosa updated = update(existing);
        log.info("Glosa updated successfully with id: {}", id);
        
        return updated;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Glosa getGlosaById(Long id) {
        log.debug("Getting glosa by id: {}", id);
        return findByIdOrThrow(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Glosa> getGlosaByDescripcion(String descripcion) {
        log.debug("Getting glosa by descripcion: {}", descripcion);
        return repository.findByDescripcion(descripcion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Glosa> getGlosasByDescripcionContaining(String keyword) {
        log.debug("Getting glosas by descripcion containing: {}", keyword);
        return repository.findByDescripcionContainingIgnoreCase(keyword);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Glosa> getAllGlosas() {
        log.debug("Getting all glosas");
        return findAll();
    }
    
    @Override
    @Transactional
    public void deleteGlosa(Long id) {
        log.debug("Deleting glosa with id: {}", id);
        deleteById(id);
        log.info("Glosa deleted successfully with id: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByDescripcion(String descripcion) {
        return repository.existsByDescripcion(descripcion);
    }
    
    @Override
    @Transactional
    public Glosa findOrCreateByDescripcion(String descripcion) {
        log.debug("Finding or creating glosa with descripcion: {}", descripcion);
        
        String normalizedDesc = descripcion.trim().toUpperCase();
        
        return getGlosaByDescripcion(normalizedDesc)
            .orElseGet(() -> {
                Glosa nuevaGlosa = Glosa.builder()
                    .descripcion(normalizedDesc)
                    .build();
                return createGlosa(nuevaGlosa);
            });
    }

    // Implementación de métodos abstractos de GenericServiceImpl
    @Override
    protected Long getId(Glosa entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(Glosa entity) {
        if (entity.getDescripcion() == null || entity.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("Descripcion cannot be null or empty");
        }
        if (entity.getDescripcion().length() > 30) {
            throw new IllegalArgumentException("Descripcion length must be less than 30 characters");
        }
    }
    
    @Override
    protected void validateBeforeUpdate(Glosa entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("Glosa not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(Glosa entity, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "descripcion" -> entity.setDescripcion((String) value);
                default -> log.warn("Unknown field for partial update: {}", key);
            }
        });
    }

    // Métodos no implementados de IGenericServiceUtils
    @Override
    public void softDelete(Long id) {
        throw new UnsupportedOperationException("Soft delete not implemented for Glosa");
    }
    
    @Override
    public void restore(Long id) {
        throw new UnsupportedOperationException("Restore not implemented for Glosa");
    }
    
    @Override
    public List<Glosa> findActive() {
        return getAllGlosas();
    }
    
    @Override
    public List<Glosa> findInactive() {
        return List.of();
    }
}
