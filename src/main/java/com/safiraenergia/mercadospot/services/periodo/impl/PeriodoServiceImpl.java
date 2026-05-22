package com.safiraenergia.mercadospot.services.periodo.impl;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.repository.IPeriodoRepository;
import com.safiraenergia.mercadospot.services.core.impl.GenericServiceImpl;
import com.safiraenergia.mercadospot.services.periodo.IPeriodoService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class PeriodoServiceImpl extends GenericServiceImpl<Periodo, Long, IPeriodoRepository> implements IPeriodoService{
    
    @Autowired
    public PeriodoServiceImpl(IPeriodoRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Periodo createPeriodo(Periodo periodo) {
        log.debug("Creating periodo for date: {}", periodo.getMes());
        
        if (existsByMes(periodo.getMes())) {
            throw new RuntimeException("Periodo already exists for date: " + periodo.getMes());
        }
        
        Periodo saved = save(periodo);
        log.info("Periodo created successfully with id: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    @Transactional
    public Periodo updatePeriodo(Long id, Periodo periodo) {
        log.debug("Updating periodo with id: {}", id);
        
        Periodo existing = findByIdOrThrow(id);
        
        if (periodo.getMes() != null && !periodo.getMes().equals(existing.getMes())) {
            if (existsByMes(periodo.getMes())) {
                throw new RuntimeException("Periodo already exists for date: " + periodo.getMes());
            }
            existing.setMes(periodo.getMes());
        }
        
        Periodo updated = update(existing);
        log.info("Periodo updated successfully with id: {}", id);
        
        return updated;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Periodo getPeriodoById(Long id) {
        log.debug("Getting periodo by id: {}", id);
        return findByIdOrThrow(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Periodo> getPeriodoByMes(Date mes) {
        log.debug("Getting periodo by mes: {}", mes);
        return repository.findByMes(mes);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Periodo> getPeriodoByYearAndMonth(int year, int month) {
        log.debug("Getting periodo by year: {}, month: {}", year, month);
        return repository.findByYearAndMonth(year, month);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Periodo> getPeriodosByYear(int year) {
        log.debug("Getting periodos by year: {}", year);
        // Implementar si es necesario un método en el repository
        return findAll().stream()
            .filter(p -> {
                Calendar cal = Calendar.getInstance();
                cal.setTime(p.getMes());
                return cal.get(Calendar.YEAR) == year;
            })
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Periodo> getAllPeriodos() {
        log.debug("Getting all periodos");
        return findAll();
    }
    
    @Override
    @Transactional
    public void deletePeriodo(Long id) {
        log.debug("Deleting periodo with id: {}", id);
        deleteById(id);
        log.info("Periodo deleted successfully with id: {}", id);
    }
    
    @Override
    @Transactional
    public Periodo findOrCreateByMes(Date mes) {
        log.debug("Finding or creating periodo for date: {}", mes);
        
        return getPeriodoByMes(mes)
            .orElseGet(() -> {
                Periodo nuevoPeriodo = Periodo.builder()
                    .mes(mes)
                    .build();
                return createPeriodo(nuevoPeriodo);
            });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Periodo getCurrentPeriodo() {
        log.debug("Getting current periodo");
        LocalDate now = LocalDate.now();
        Date currentDate = Date.valueOf(now);
        
        return getPeriodoByMes(currentDate)
            .orElseThrow(() -> new RuntimeException("No periodo found for current date"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByMes(Date mes) {
        return repository.existsByMes(mes);
    }

    // Implementación de métodos abstractos de GenericServiceImpl
    
    @Override
    protected Long getId(Periodo entity) {
        return entity.getId();
    }
    
    @Override
    protected void validateBeforeSave(Periodo entity) {
        if (entity.getMes() == null) {
            throw new IllegalArgumentException("Mes cannot be null");
        }
    }
    
    @Override
    protected void validateBeforeUpdate(Periodo entity) {
        validateBeforeSave(entity);
    }
    
    @Override
    protected void validateBeforeDelete(Long id) {
        if (!existsById(id)) {
            throw new RuntimeException("Periodo not found with id: " + id);
        }
    }
    
    @Override
    protected void applyUpdates(Periodo entity, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "mes" -> entity.setMes((Date) value);
                default -> log.warn("Unknown field for partial update: {}", key);
            }
        });
    }
    
    // Métodos no implementados de IGenericServiceUtils
    @Override
    public void softDelete(Long id) {
        throw new UnsupportedOperationException("Soft delete not implemented for Periodo");
    }
    
    @Override
    public void restore(Long id) {
        throw new UnsupportedOperationException("Restore not implemented for Periodo");
    }
    
    @Override
    public List<Periodo> findActive() {
        return getAllPeriodos();
    }
    
    @Override
    public List<Periodo> findInactive() {
        return List.of();
    }
}
