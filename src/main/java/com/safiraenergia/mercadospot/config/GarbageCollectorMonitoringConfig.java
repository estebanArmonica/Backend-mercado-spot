package com.safiraenergia.mercadospot.config;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@EnableScheduling
public class GarbageCollectorMonitoringConfig {
    private final Map<String, Long> gcCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> gcTime = new ConcurrentHashMap<>();

    public GarbageCollectorMonitoringConfig(){
        registerGCMonitoring();
    }

    private void registerGCMonitoring() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean instanceof NotificationEmitter emitter) {
                emitter.addNotificationListener(new NotificationListener() {
                    @Override
                    public void handleNotification(Notification notification, Object handback) {
                        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
                            GcInfo gcInfo = info.getGcInfo();
                            
                            String gcName = info.getGcName();
                            String gcAction = info.getGcAction();
                            long duration = gcInfo.getDuration();
                            long startTime = gcInfo.getStartTime();
                            long endTime = gcInfo.getEndTime();
                            
                            // Registrar estadísticas
                            gcCounts.merge(gcName, 1L, Long::sum);
                            gcTime.merge(gcName, duration, Long::sum);
                            
                            // Log para GC largos (> 1 segundo)
                            if (duration > 1000) {
                                log.warn("⚠️ GC Largo detectado: {} - Duración: {}ms, Acción: {}, Memoria antes: {}MB, después: {}MB",
                                    gcName, duration, gcAction,
                                    gcInfo.getMemoryUsageBeforeGc().values().stream().mapToLong(mu -> mu.getUsed() / 1024 / 1024).sum(),
                                    gcInfo.getMemoryUsageAfterGc().values().stream().mapToLong(mu -> mu.getUsed() / 1024 / 1024).sum()
                                );
                            } else {
                                log.debug("📊 GC: {} - Duración: {}ms, Acción: {}",
                                    gcName, duration, gcAction);
                            }
                        }
                    }
                }, null, null);
            }
        }
        log.info("✅ Monitoreo de Garbage Collector registrado");
    }

    @Scheduled(fixedDelay = 60000) // por cada minuto
    public void logGCStatistics() {
        if(!gcCounts.isEmpty()) {
            log.info("📊 Estadísticas de Garbage Collector:");
            for(Map.Entry<String, Long> entry: gcCounts.entrySet()) {
                String gcName = entry.getKey();
                long count = entry.getValue();
                long totalTime = gcTime.getOrDefault(gcName,0L);
                long avgTime = count > 0 ? totalTime / count: 0;

                log.info("   -{}: {} colecciones, tiempo total: {}ms, promedio: {}ms", gcName, count, totalTime, avgTime);
            }
        }

        // Log de memoria
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        log.info("💾 Memoria actual: {}MB / {}MB usado ({}%)", usedMemory, maxMemory, (usedMemory * 100 / maxMemory));
    }
}
