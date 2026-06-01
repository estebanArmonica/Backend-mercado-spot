#!/bin/bash

echo "🚀 Iniciando aplicación con monitoreo de Garbage Collector..."

# configuracion del GC para el ETL
JAVA_OPTS="-Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1ReservePercent=20 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseStringDeduplication \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -Xloggc:logs/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=logs/heapdump.hprof"

echo "📊 Configuración de GC:"
echo "   - Heap incial: 2GB"
echo "   - Heap máximo: 4GB"
echo "   - GC: G1GC"
echo "   - Logs: logs/gc.log"

java $JAVA_OPTS -jar target/*.jar --spring.profiles.active=prod