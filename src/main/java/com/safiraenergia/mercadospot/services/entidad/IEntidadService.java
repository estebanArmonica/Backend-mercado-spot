package com.safiraenergia.mercadospot.services.entidad;

import java.util.List;
import java.util.Optional;

import com.safiraenergia.mercadospot.models.Entidad;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

/**
 * Interfaz específica para operaciones de Entidad
 * Aplicando Principios de Segregación de Interfaces (ISP)
 */
public interface IEntidadService extends IGenericServiceUtils<Entidad, Long>{
    Entidad createEntidad(Entidad entidad);
    Entidad updateEntidad(Long id, Entidad entidad);
    Entidad getEntidadById(Long id);
    Optional<Entidad> getEntidadByRut(String rut);
    List<Entidad> getEntidadesByNombre(String nombre);
    List<Entidad> getAllEntidades();
    void deleteEntidad(Long id);
    boolean existsByRut(String rut);

    // métodos adicionales (por el momento los dejare usados)
    Entidad findOrCreateByRut(String rut, String nombre);
    List<Entidad> getEntidadesActivas();
}
