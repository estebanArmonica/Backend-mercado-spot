package com.safiraenergia.mercadospot.services.tipoEntidad;

import com.safiraenergia.mercadospot.models.TipoEntidad;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz específica para operaciones de TipoEntidad
 * Aplicando Principio de Segregación de Interfaces (ISP)
 */
public interface ITipoEntidadService extends IGenericServiceUtils<TipoEntidad, Long>{
    
    TipoEntidad createTipoEntidad(TipoEntidad tipoEntidad);
    
    TipoEntidad updateTipoEntidad(Long id, TipoEntidad tipoEntidad);
    
    TipoEntidad getTipoEntidadById(Long id);
    
    Optional<TipoEntidad> getTipoEntidadByTipoRol(String tipoRol);
    
    List<TipoEntidad> getAllTipoEntidades();
    
    List<TipoEntidad> getTipoEntidadesByTipoRolContaining(String keyword);
    
    void deleteTipoEntidad(Long id);
    
    boolean existsByTipoRol(String tipoRol);
    
    // Métodos adicionales
    TipoEntidad findOrCreateByTipoRol(String tipoRol);
    
    List<TipoEntidad> getTipoEntidadesOrdenadas();
    
    // Métodos para tipos específicos si los necesitas
    Optional<TipoEntidad> getTipoEntidadCliente();
    Optional<TipoEntidad> getTipoEntidadProveedor();
    Optional<TipoEntidad> getTipoEntidadEmpleado();
}
