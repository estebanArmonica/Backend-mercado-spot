package com.safiraenergia.mercadospot.services.estado.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.repository.IEstadoRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.estado.IEstadoService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class EstadoServiceImpl extends GenericServiceImpl<Estado, Long, IEstadoRepository> implements IEstadoService{
    
    @Autowired
    public EstadoServiceImpl(IEstadoRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Estado createEstado(Estado estado) {
        log.debug("Creating estado with descripcion: {}", estado.getDescripcion());
        
        if (existsByDescripcion(estado.getDescripcion())) {
            throw new RuntimeException("Estado already exists with descripcion: " + estado.getDescripcion());
        }
        
        Estado saved = save(estado);
        log.info("Estado created successfully with id: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public Estado updateEstado(Long id, Estado estado) {
        log.debug("Updating estado with id: {}", id);
        
        Estado existing = findByIdOrThrow(id);
        
        if (estado.getDescripcion() != null && !estado.getDescripcion().equals(existing.getDescripcion())) {
            if (existsByDescripcion(estado.getDescripcion())) {
                throw new RuntimeException("Descripcion already exists: " + estado.getDescripcion());
            }
            existing.setDescripcion(estado.getDescripcion());
        }
        
        Estado updated = update(existing);
        log.info("Estado updated successfully with id: {}", id);
        
        return updated;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Estado getEstadoById(Long id) {
        log.debug("Getting estado by id: {}", id);
        return findByIdOrThrow(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Estado> getEstadoByDescripcion(String descripcion) {
        log.debug("Getting estado by descripcion: {}", descripcion);
        return repository.findByDescripcion(descripcion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Estado> getAllEstados() {
        log.debug("Getting all estados");
        return findAll();
    }
    
    @Override
    @Transactional
    public void deleteEstado(Long id) {
        log.debug("Deleting estado with id: {}", id);
        deleteById(id);
        log.info("Estado deleted successfully with id: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByDescripcion(String descripcion) {
        return repository.existsByDescripcion(descripcion);
    }
    
    @Override
    @Transactional
    public Estado findOrCreateByDescripcion(String descripcion) {
        log.debug("Finding or creating estado with descripcion: {}", descripcion);
        
        String normalizedDesc = descripcion.trim().toUpperCase();
        
        return getEstadoByDescripcion(normalizedDesc)
            .orElseGet(() -> {
                Estado nuevoEstado = Estado.builder()
                    .descripcion(normalizedDesc)
                    .build();
                return createEstado(nuevoEstado);
            });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Estado getEstadoPendiente() {
        log.debug("Getting estado PENDIENTE");
        return getEstadoByDescripcion("PENDIENTE")
            .orElseThrow(() -> new RuntimeException("Estado PENDIENTE not found"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Estado getEstadoPagada() {
        log.debug("Getting estado PAGADA");
        return getEstadoByDescripcion("PAGADA")
            .orElseThrow(() -> new RuntimeException("Estado PAGADA not found"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Estado getEstadoVencida() {
        log.debug("Getting estado VENCIDA");
        return getEstadoByDescripcion("VENCIDA")
            .orElseThrow(() -> new RuntimeException("Estado VENCIDA not found"));
    }
    
    // Implementación de métodos abstractos de GenericServiceImpl
    
    @Override
    protected Long getId(Estado entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(Estado entity) {
        if (entity.getDescripcion() == null || entity.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("Descripcion cannot be null or empty");
        }
        if (entity.getDescripcion().length() > 15) {
            throw new IllegalArgumentException("Descripcion length must be less than 15 characters");
        }
    }
    
    @Override
    protected void validateBeforeUpdate(Estado entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("Estado not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(Estado entity, Map<String, Object> updates) {
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
        throw new UnsupportedOperationException("Soft delete not implemented for Estado");
    }
    
    @Override
    public void restore(Long id) {
        throw new UnsupportedOperationException("Restore not implemented for Estado");
    }
    
    @Override
    public List<Estado> findActive() {
        return getAllEstados();
    }
    
    @Override
    public List<Estado> findInactive() {
        return List.of();
    }
}
