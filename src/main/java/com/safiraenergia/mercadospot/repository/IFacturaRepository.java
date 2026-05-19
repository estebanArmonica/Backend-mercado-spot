package com.safiraenergia.mercadospot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.safiraenergia.mercadospot.models.Factura;

import java.util.List;

@Repository
public interface IFacturaRepository extends JpaRepository<Factura, Long>, JpaSpecificationExecutor<Factura>{
    boolean existsByFolio(int folio);

    @Query("SELECT COUNT(f) FROM Factura f WHERE f.folio = :folio AND f.periodo.id = :periodoId")
    long countByFolioAndPeriodo(@Param("folio") int folio, @Param("periodoId") Long periodoId);

    // consulta especifica por el rut de la entidad
    List<Factura> findByEntidadRutEntidad(String rutEntidad);

    @Query("SELECT f FROM Factura f WHERE YEAR(f.periodo.mes) = :year AND MONTH(f.periodo.mes) = :month")
    List<Factura> findByPeriodoYearAndPeriodoMonth(@Param("year") int year, @Param("month") int month);

    // Método de estadistica
    @Query("SELECT SUM(f.montoTotal) FROM Factura f")
    Double sumMontoTotal();
    
    @Query("SELECT AVG(f.montoTotal) FROM Factura f")
    Double avgMontoTotal();

    @Query("SELECT SUM(f.montoTotal) FROM Factura f WHERE f.fechaEmision BETWEEN :fechaInicio AND :fechaFin")
    Double sumMontoTotalByFechaEmisionBetween(@Param("fechaInicio") java.sql.Date fechaInicio, 
                                              @Param("fechaFin") java.sql.Date fechaFin);
}
