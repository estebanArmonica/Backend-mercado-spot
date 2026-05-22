package com.safiraenergia.mercadospot.repository;

import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.TipoEntidad;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ITipoEntidadRepository extends JpaRepository<TipoEntidad, Long>{
    Optional<TipoEntidad> findByTipoRol(String tipoRol);
    
    List<TipoEntidad> findByTipoRolContainingIgnoreCase(String tipoRol);
    
    boolean existsByTipoRol(String tipoRol);
    
    List<TipoEntidad> findAllByOrderByTipoRolAsc();
}
