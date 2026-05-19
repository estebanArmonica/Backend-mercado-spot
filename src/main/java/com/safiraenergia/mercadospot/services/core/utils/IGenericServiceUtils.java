package com.safiraenergia.mercadospot.services.core.utils;

import java.util.List;
import java.util.Map;

import com.safiraenergia.mercadospot.services.core.ICrudService;

/**
 * Interfaz genérica con operaciones adicionales
 * Principios de Abierto/Cerrado (OCP) 
*/
public interface IGenericServiceUtils<T, ID> extends ICrudService<T, ID> {
    T findByIdOrThrow(ID id);
    T updatePartial(ID id, Map<String, Object> updates);
    void softDelete(ID id);
    void restore(ID id);
    List<T> findActive();
    List<T> findInactive();
}
