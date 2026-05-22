package com.safiraenergia.mercadospot.dto.glosa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlosaDTO {
    private Long id;
    private String descripcion;
}
