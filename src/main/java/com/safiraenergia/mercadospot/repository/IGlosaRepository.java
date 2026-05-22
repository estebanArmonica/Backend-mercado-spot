package com.safiraenergia.mercadospot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.Glosa;

@Repository
public interface IGlosaRepository extends JpaRepository<Glosa, Long>{
    Optional<Glosa> findByDescripcion(String descripcion);
    List<Glosa> findByDescripcionContainingIgnoreCase(String descripcion);
    boolean existsByDescripcion(String descripcion);
}
