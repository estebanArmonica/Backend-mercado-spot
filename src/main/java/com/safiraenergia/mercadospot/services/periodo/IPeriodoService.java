package com.safiraenergia.mercadospot.services.periodo;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.safiraenergia.mercadospot.models.Periodo;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

/**
 * Interfaz específica para operaciones de Periodo
 * Aplicando Principio de Segregación de Interfaces (ISP)
*/
public interface IPeriodoService extends IGenericServiceUtils<Periodo, Long>{
    Periodo createPeriodo(Periodo periodo);
    Periodo updatePeriodo(Long id, Periodo periodo);
    Periodo getPeriodoById(Long id);
    Optional<Periodo> getPeriodoByMes(Date mes);
    Optional<Periodo> getPeriodoByYearAndMonth(int year, int month);
    List<Periodo> getPeriodosByYear(int year);
    List<Periodo> getAllPeriodos();
    void deletePeriodo(Long id);
    
    // Métodos adicionales
    Periodo findOrCreateByMes(Date mes);
    Periodo getCurrentPeriodo();
    boolean existsByMes(Date mes);
}
