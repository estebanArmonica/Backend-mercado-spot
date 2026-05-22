package com.safiraenergia.mercadospot.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.safiraenergia.mercadospot.enums.ETLStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETLResultDTO {
    private String jobId;
    private ETLStatus status;
    private String fileName;
    private int totalRecordsExtracted;
    private int totalRecordsTransformed;
    private int totalRecordsLoaded;
    private int totalRecordsUpdated;
    private int totalRecordsSkipped;
    private int totalErrors;
    private List<String> errors;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
