package com.safiraenergia.mercadospot.dto.factura;

import java.sql.Date;

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
    private String estado, tipoEntidad, estadoOriginal;
}
