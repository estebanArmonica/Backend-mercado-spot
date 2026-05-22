package com.safiraenergia.mercadospot.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaFilterDTO {
    private Long folio;
    private String rutEntidad;
    private Integer year;
    private Integer month;
    private java.sql.Date fechaEmisionDesde;
    private java.sql.Date fechaEmisionHasta;
    private Double montoMinimo;
    private Double montoMaximo;
    private String sortBy;
    private String sortDirection;
}
