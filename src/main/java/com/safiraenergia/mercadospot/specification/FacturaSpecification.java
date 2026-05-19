package com.safiraenergia.mercadospot.specification;

import com.safiraenergia.mercadospot.dto.FacturaFilterDTO;
import com.safiraenergia.mercadospot.models.Factura;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
/**
 * Especificaciones para consultas dinámicas de Factura
 * Aplicando Patrón Specification y Principio de Responsabilidad Única (SRP)
*/
@Component
public class FacturaSpecification {

    /**
     * Crea una especificación para filtrar facturas según los criterios del DTO
     * @Param filter DTO con los filtros a aplicar
     * @return Specification<Factura> para usar en consultas JPA
    */
   public static Specification<Factura> withFilters(FacturaFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(filter == null){
                return criteriaBuilder.conjunction();
            }

            // Filtro por folio
            if(filter.getFolio() != null && filter.getFolio() > 0){
                predicates.add(criteriaBuilder.equal(root.get("folio"), filter.getFolio().intValue()));
            }

            // filtro por RUT entidad
            if (StringUtils.hasText(filter.getRutEntidad())) {
                predicates.add(criteriaBuilder.equal(
                    root.get("entidad").get("rut"), 
                    filter.getRutEntidad()
                ));
            }

            // Filtro por año y mes del periodo
            if (filter.getYear() != null && filter.getYear() > 0) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("YEAR", Integer.class, root.get("periodo").get("mes")),
                    filter.getYear()
                ));
            }

            if (filter.getMonth() != null && filter.getMonth() > 0) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("MONTH", Integer.class, root.get("periodo").get("mes")),
                    filter.getMonth()
                ));
            }

            // Filtro por rango de fechas de emisión
            if (filter.getFechaEmisionDesde() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("fechaEmision"), 
                    filter.getFechaEmisionDesde()
                ));
            }
            
            if (filter.getFechaEmisionHasta() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("fechaEmision"), 
                    filter.getFechaEmisionHasta()
                ));
            }

            // Filtro por rango de montos
            if (filter.getMontoMinimo() != null && filter.getMontoMinimo() > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("montoTotal"), 
                    filter.getMontoMinimo()
                ));
            }
            
            if (filter.getMontoMaximo() != null && filter.getMontoMaximo() > 0) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("montoTotal"), 
                    filter.getMontoMaximo()
                ));
            }

            /*  Ordenamiento
            if (filter.getSortBy() != null && !filter.getSortBy().isEmpty()) {
                if ("asc".equalsIgnoreCase(filter.getSortDirection())) {
                    query.orderBy(criteriaBuilder.asc(root.get(filter.getSortBy())));
                } else {
                    query.orderBy(criteriaBuilder.desc(root.get(filter.getSortBy())));
                }
            }*/

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
   }

   /**
     * Especificación para facturas por entidad
    */
   public static Specification<Factura> byEntidadRut(String rutEntidad) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("entidad").get("rut"), rutEntidad);
   }

   /**
     * Especificación para facturas por periodo
    */
   public static Specification<Factura> byPeriodo(int year, int month) {
        return (root, query, criteriaBuilder) -> {
            Predicate yearPredicate = criteriaBuilder.equal(
                criteriaBuilder.function("YEAR", Integer.class, root.get("periodo").get("mes")),
                year
            );
            Predicate monthPredicate = criteriaBuilder.equal(
                criteriaBuilder.function("MONTH", Integer.class, root.get("periodo").get("mes")),
                month
            );
            return criteriaBuilder.and(yearPredicate, monthPredicate);
        };
   }

   /**
     * Especificación para facturas por rango de fechas
    */
    public static Specification<Factura> byFechaEmisionBetween(java.sql.Date desde, java.sql.Date hasta) {
        return (root, query, criteriaBuilder) -> {
            if (desde != null && hasta != null) {
                return criteriaBuilder.between(root.get("fechaEmision"), desde, hasta);
            } else if (desde != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("fechaEmision"), desde);
            } else if (hasta != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("fechaEmision"), hasta);
            }
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Especificación para facturas por monto total mayor a
     */
    public static Specification<Factura> byMontoTotalGreaterThan(Double monto) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.greaterThan(root.get("montoTotal"), monto);
    }
    
    /**
     * Especificación para facturas por monto total menor a
     */
    public static Specification<Factura> byMontoTotalLessThan(Double monto) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.lessThan(root.get("montoTotal"), monto);
    }
    
    /**
     * Especificación para facturas pagadas (fecha de pago no nula)
     */
    public static Specification<Factura> isPagada() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNotNull(root.get("fechaPago"));
    }
    
    /**
     * Especificación para facturas no pagadas (fecha de pago nula)
     */
    public static Specification<Factura> isNoPagada() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isNull(root.get("fechaPago"));
    }
    
    /**
     * Combina múltiples especificaciones con AND
     */
    public static Specification<Factura> and(Specification<Factura>... specs) {
       Specification<Factura> result = (root, query, cb) -> cb.conjunction();
        for (Specification<Factura> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    /**
     * Combina múltiples especificaciones con OR
     */
    public static Specification<Factura> or(Specification<Factura>... specs) {
        Specification<Factura> result = (root, query, cb) -> cb.conjunction();
        for (Specification<Factura> spec : specs) {
            if (spec != null) {
                result = result.or(spec);
            }
        }
        return result;
    }
}
