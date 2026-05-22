package com.safiraenergia.mercadospot.services.estado;

import java.util.List;
import java.util.Optional;

import com.safiraenergia.mercadospot.models.Estado;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

/**
 * Interfaz específica para operaciones de Estado
 * Aplicando Principio de Segregación de Interfaces (ISP)
 */
public interface IEstadoService extends IGenericServiceUtils<Estado, Long>{
    
    Estado createEstado(Estado estado);
    Estado updateEstado(Long id, Estado estado);
    Estado getEstadoById(Long id);
    Optional<Estado> getEstadoByDescripcion(String descripcion);
    List<Estado> getAllEstados();
    void deleteEstado(Long id);
    boolean existsByDescripcion(String descripcion);
    
    // Métodos adicionales
    Estado findOrCreateByDescripcion(String descripcion);
    Estado getEstadoPendiente();
    Estado getEstadoPagada();
    Estado getEstadoVencida();
}
