package com.safiraenergia.mercadospot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.Estado;

@Repository
public interface IEstadoRepository extends JpaRepository<Estado, Long>, JpaSpecificationExecutor<Estado>{
    Optional<Estado> findByDescripcion(String descripcion);
    boolean existsByDescripcion(String descripcion);
}
