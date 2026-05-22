package com.safiraenergia.mercadospot.services.entidad.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.repository.IEntidadRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.entidad.IEntidadService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class EntidadServiceImpl extends GenericServiceImpl<Entidad, Long, IEntidadRepository> implements IEntidadService {
    
    @Autowired
    public EntidadServiceImpl(IEntidadRepository repository) {
        super(repository);
    }
    
    @Override
    @Transactional
    public Entidad createEntidad(Entidad entidad){
        log.debug("Creating entidad with RUT: {}", entidad.getRutEntidad());
        
        if(existsByRut(entidad.getRutEntidad())){
            throw new RuntimeException("Entidad already exists with RUT: " + entidad.getRutEntidad());
        }

        Entidad saved = save(entidad);
        log.info("Entidad created successfully with id: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Entidad updateEntidad(Long id, Entidad entidad){
        log.debug("Updating entidad with id: {}", id);
        Entidad existing = findByIdOrThrow(id);

        if (entidad.getRutEntidad() != null && !entidad.getRutEntidad().equals(existing.getRutEntidad())) {
            if (existsByRut(entidad.getRutEntidad())) {
                throw new RuntimeException("RUT already exists: " + entidad.getRutEntidad());
            }
            existing.setRutEntidad(entidad.getRutEntidad());
        }

        Entidad updated = update(existing);
        log.info("Entidad updated successfully with id: {}", id);

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Entidad getEntidadById(Long id){
        log.debug("Getting entidad by id: {}", id);
        return findByIdOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Entidad> getEntidadByRut(String rut) {
        log.debug("Getting entidad by RUT: {}", rut);
        return repository.findByRutEntidad(rut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entidad> getEntidadesByNombre(String nombre){
        log.debug("Getting entidades by nombre: {}", nombre);

        // implementacion si es necesario crear un método
        return findAll().stream()
            .filter(e -> e.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entidad> getAllEntidades() {
        log.debug("Getting all entidades");
        return findAll();
    }

    @Override
    @Transactional
    public void deleteEntidad(Long id) {
        log.debug("Deleting entidad with id: {}", id);
        deleteById(id);
        log.info("Entidad deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByRut(String rut) {
        return repository.existsByRutEntidad(rut);
    }
    
    @Override
    @Transactional
    public Entidad findOrCreateByRut(String rut, String nombre) {
        log.debug("Finding or creating entidad with RUT: {}", rut);
        
        return repository.findByRutEntidad(rut)
            .orElseGet(() -> {
                Entidad nuevaEntidad = Entidad.builder()
                    .rutEntidad(rut)
                    .nombre(nombre)
                    .build();
                return createEntidad(nuevaEntidad);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entidad> getEntidadesActivas() {
        log.debug("Getting active entidades");
        // Si tuvieras un campo 'activo', lo filtrarías aquí
        return findAll();
    }

    //---------------------------------------------------------------------------------------------------

    // Implementación de métodos abstractos de GenericServiceImpl
    @Override
    protected Long getId(Entidad entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(Entidad entity) {
        if (entity.getRutEntidad() == null || entity.getRutEntidad().trim().isEmpty()) {
            throw new IllegalArgumentException("RUT cannot be null or empty");
        }
        if (entity.getNombre() == null || entity.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre cannot be null or empty");
        }
        if (entity.getRutEntidad().length() > 12) {
            throw new IllegalArgumentException("RUT length must be less than 12 characters");
        }
        if (entity.getNombre().length() > 50) {
            throw new IllegalArgumentException("Nombre length must be less than 50 characters");
        }
    }

    @Override
    protected void validateBeforeUpdate(Entidad entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("Entidad not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(Entidad entity, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "nombre" -> entity.setNombre((String) value);
                case "rutEntidad" -> entity.setRutEntidad((String) value);
                default -> log.warn("Unknown field for partial update: {}", key);
            }
        });
    }

    //------------------------------------------------------------------------------------------------------

    // Métodos no implementados de IGenericServiceUtils
    @Override
    public void softDelete(Long id) {
        throw new UnsupportedOperationException("Soft delete not implemented for Entidad");
    }
    
    @Override
    public void restore(Long id) {
        throw new UnsupportedOperationException("Restore not implemented for Entidad");
    }
    
    @Override
    public List<Entidad> findActive() {
        return getEntidadesActivas();
    }
    
    @Override
    public List<Entidad> findInactive() {
        return List.of();
    }
}
