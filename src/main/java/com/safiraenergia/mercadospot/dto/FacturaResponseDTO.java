package com.safiraenergia.mercadospot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponseDTO {
    private Long id;
    private Long folio;
    private Integer montoNeto;
    private Integer montoBruto;
    private Integer montoTotal;
    private java.util.Date fechaEmision;
    private java.util.Date fechaPago;
    private String rutEntidad;
    private String nombreEntidad;
    private String glosa;
    private java.util.Date periodo;
}
