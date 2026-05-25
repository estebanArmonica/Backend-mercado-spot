package com.safiraenergia.mercadospot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ETLLogger {

    private static final String ETL_LOG_FILE = "logs/etl-process.log";
    private static final String ERROR_LOG_FILE = "logs/etl-errors.log";
    private static final String SUCCESS_LOG_FILE = "logs/etl-success.log";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public synchronized void logError(String jobId, String message, Exception e) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] [JOB:%s] ERROR: %s - %s%n", timestamp, jobId, message, e.getMessage());
    
        // Log a archivo
        writeToFile(ERROR_LOG_FILE, logMessage);
        // También a consola
        log.error("{} - {}", message, e.getMessage());
    }

    public synchronized void logSuccess(String jobId, String message, int recordsInserted) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] [JOB:%s] SUCCESS: %s - Records inserted: %d%n", timestamp, jobId, message, recordsInserted);
        
        writeToFile(SUCCESS_LOG_FILE, logMessage);
        log.info("JOB {}: {} - {} records inserted", jobId, message, recordsInserted);
    }

    public synchronized void logInfo(String jobId, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] [JOB:%s] INFO: %s%n", timestamp, jobId, message);
        
        writeToFile(ETL_LOG_FILE, logMessage);
        log.info("JOB {}: {}", jobId, message);
    }

    public synchronized void logWarning(String jobId, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] [JOB:%s] WARNING: %s%n", timestamp, jobId, message);
        
        writeToFile(ETL_LOG_FILE, logMessage);
        log.warn("JOB {}: {}", jobId, message);
    }
    
    private void writeToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content);
        } catch (IOException e) {
            log.error("Failed to write to log file: {}", filename, e);
        }
    }
}
