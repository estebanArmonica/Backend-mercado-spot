package com.safiraenergia.mercadospot.dto.tipoentidad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoEntidadDTO {
    private Long id;
    private String tipoRol;
}
