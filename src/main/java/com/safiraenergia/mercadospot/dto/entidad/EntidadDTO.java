package com.safiraenergia.mercadospot.dto.entidad;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntidadDTO {
    private Long id;
    private String rutEntidad;
    private String nombre;
    private List<String> tiposEntidad;
}
