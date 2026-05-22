package com.safiraenergia.mercadospot.dto.periodo;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoDTO {
    private Long id;
    private Date mes;
    private Integer year;
    private Integer month;
}
