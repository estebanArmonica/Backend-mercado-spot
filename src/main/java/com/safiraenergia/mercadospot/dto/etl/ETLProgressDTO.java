package com.safiraenergia.mercadospot.dto.etl;

import java.time.LocalDateTime;

import com.safiraenergia.mercadospot.enums.ETLStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETLProgressDTO {
    private String jobId;
    private String usuarioId;
    private String fileName;
    private ETLStatus status;
    private String currentStep;
    private int progress;
    private int recordsExtracted;
    private int recordsTransformed;
    private int recordsLoaded;
    private String errorMessage;
    private LocalDateTime startTime;
}
