package com.safiraenergia.mercadospot.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

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
    private Date fechaEmision;
    private Date fechaPago;
    private String rutEntidad;
    private String nombreEntidad;
    private String glosa;
    private String periodo;
}
