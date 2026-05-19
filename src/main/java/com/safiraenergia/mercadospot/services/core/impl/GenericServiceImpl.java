package com.safiraenergia.mercadospot.services.core.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

import jakarta.persistence.EntityNotFoundException;

/**
 *  Implementación genérica de servicios
 *  Aplicando Patrón Template Method y Principios de Inversión de Dependencias (DIP)
*/
@Transactional
public abstract class GenericServiceImpl<T, ID, R extends JpaRepository<T, ID>> implements IGenericServiceUtils<T, ID>{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final R repository;
    
    public GenericServiceImpl(R repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public T save(T entity){
        log.debug("Saving entity: {}", entity);
        validateBeforeSave(entity);
        T saved = repository.save(entity);
        log.info("Entity saved successfully with id: {}", getId(saved));
        return saved;
    }

    @Override
    @Transactional
    public List<T> saveAll(List<T> entities) {
        log.debug("Saving {} entities", entities.size());
        entities.forEach(this::validateBeforeSave);
        List<T> saved = repository.saveAll(entities);
        log.info("{} entities saved successfully", saved.size());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        log.debug("Finding entity by id: {}", id);
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public T findByIdOrThrow(ID id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Entity with id %s not found", id)));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        log.debug("Finding all entities");
        return repository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<T> findAll(List<ID> ids) {
        log.debug("Finding entities by ids: {}", ids);
        return repository.findAllById(ids);
    }

    @Override
    @Transactional
    public T update(T entity) {
        log.debug("Updating entity: {}", entity);
        validateBeforeUpdate(entity);
        T updated = repository.save(entity);
        log.info("Entity updated successfully");
        return updated;
    }
    
    @Override
    @Transactional
    public void deleteById(ID id) {
        log.debug("Deleting entity by id: {}", id);
        validateBeforeDelete(id);
        repository.deleteById(id);
        log.info("Entity with id {} deleted successfully", id);
    }
    
    @Override
    @Transactional
    public void delete(T entity) {
        log.debug("Deleting entity: {}", entity);
        repository.delete(entity);
        log.info("Entity deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    // Métodos template para que las subclases implementen
    protected abstract ID getId(T entity);
    protected abstract void validateBeforeSave(T entity);
    protected abstract void validateBeforeUpdate(T entity);
    protected abstract void validateBeforeDelete(ID id);

    //Método sobrescrito (se puede modificar)
    @Override
    @Transactional
    public T updatePartial(ID id, Map<String, Object> updates){
        log.debug("Partially updating entity with id: {}", id);
        T entity = findByIdOrThrow(id);
        applyUpdates(entity, updates);
        return update(entity);
    }

    // Método para aplicar actualizaciones parciales
    protected abstract void applyUpdates(T entity, Map<String, Object> updates);
}
