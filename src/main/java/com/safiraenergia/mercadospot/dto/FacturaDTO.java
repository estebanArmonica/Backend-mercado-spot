package com.safiraenergia.mercadospot.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDTO {
    private Long folio;
    private int montoNeto;
    private int montoBruto;
    private int montoTotal;
    private Date fechaEmision;
    private Date fechaPago;
    private Date periodo;
    private String glosa, rutEntidad, nomEntidad;
}
