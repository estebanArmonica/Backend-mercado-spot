package com.safiraenergia.mercadospot.repository;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.Periodo;

@Repository
public interface IPeriodoRepository extends JpaRepository<Periodo, Long>{
    
    // extraccion de año y mes
    @Query("SELECT p FROM Periodo p WHERE EXTRACT(YEAR FROM p.mes) = :year AND EXTRACT(MONTH FROM p.mes) = :month")
    Optional<Periodo> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    Optional<Periodo> findByMes(Date mes);
}
