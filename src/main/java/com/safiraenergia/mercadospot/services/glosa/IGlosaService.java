package com.safiraenergia.mercadospot.services.glosa;

import java.util.List;
import java.util.Optional;

import com.safiraenergia.mercadospot.models.Glosa;
import com.safiraenergia.mercadospot.services.core.utils.IGenericServiceUtils;

/**
 * Interfaz específica para operaciones de Glosa
 * Aplicando Principio de Segregación de Interfaces (ISP)
*/
public interface IGlosaService extends IGenericServiceUtils<Glosa, Long> {
    Glosa createGlosa(Glosa glosa);
    Glosa updateGlosa(Long id, Glosa glosa);
    Glosa getGlosaById(Long id);
    Optional<Glosa> getGlosaByDescripcion(String descripcion);
    List<Glosa> getGlosasByDescripcionContaining(String keyword);
    List<Glosa> getAllGlosas();
    void deleteGlosa(Long id);
    boolean existsByDescripcion(String descripcion);
    
    // Métodos adicionales
    Glosa findOrCreateByDescripcion(String descripcion);
}
