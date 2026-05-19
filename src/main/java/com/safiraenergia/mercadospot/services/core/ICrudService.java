package com.safiraenergia.mercadospot.services.core;

import java.util.List;
import java.util.Optional;

/*
 *  Interfaz genérica para operaciones CRUD báscias
 *  Aplicando Principios de Segregación de Interfaces (ISP)
 *  @Param <T> Tipo de entidad
 *  @Param <ID> Tipo del ID
*/
public interface ICrudService<T, ID> {
    T save(T entity);
    List<T> saveAll(List<T> entities);
    Optional<T> findById(ID id);
    List<T> findAll();
    List<T> findAll(List<ID> ids);
    T update(T entity);
    void deleteById(ID id);
    void delete(T entity);
    boolean existsById(ID id);
    long count();
}
