#!/bin/bash

echo "Análisis de logs de Garbage Collector"

if [ -f "logs/gc.log" ]; then
    echo ""
    echo "=== Resumen de GC ==="

    # Total de colecciones
    TOTAL_GC=$(grep -c "GC" logs/gc.log)
    echo "Total de colecciones: $TOTAL_GC"

    # GC por tipos
    YOUNG_GC=$(grep -c "GC (G1 Young Generation)" logs/gc.log)
    FULL_GC=$(grep -c "GC (G1 Old Generation)" logs/gc.log)
    
    echo "Young GC: $YOUNG_GC"    
    echo "Full GC: $FULL_GC"

    # Tiempo promedio
    AVG_TIME=$(grep -oP "Real:\s+\K[0-9.]+" logs/gc.log | awk '{sum+=$1; count++} END {print sum/count}')
    echo "Tiempo promedio de GC: ${AVG_TIME}ms"

    # Gcs
    echo ""
    echo "=== Ultimas 5 GCs ==="
    grep "Real:" logs/gc.log | tail -5
else 
    echo "❌ No se encontró el archivo logs/gc.log"
fi