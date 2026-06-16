#!/bin/bash

# ============================================================================
# Script simplificado de lanzamiento del Backend de Mercado Spot ETL
# ============================================================================

# Configuración de colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Variables de configuración
APP_NAME="Backend-ETL-Mercado-Spot"
LOG_FILE="logs/application.log"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuración de Java
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/heapdump.hprof"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Iniciando $APP_NAME${NC}"
echo -e "${BLUE}========================================${NC}"

# Crear directorios necesarios
mkdir -p logs uploads backups
echo -e "${GREEN}✓ Directorios creados: logs, uploads, backups${NC}"

# Cargar variables de entorno
ENV_FILE="${APP_DIR}/.env"
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}✓ Cargando variables de entorno desde .env${NC}"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo -e "${YELLOW}⚠ No se encontró archivo .env${NC}"
fi

# Buscar el JAR más reciente
JAR_FILE=$(find target -name "*.jar" -type f 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${YELLOW}⚠ No se encontró el archivo JAR. Compilando...${NC}"
    
    # Verificar Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven no está instalado${NC}"
        exit 1
    fi
    
    # Compilar
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Error en la compilación${NC}"
        exit 1
    fi
    
    JAR_FILE=$(find target -name "*.jar" -type f | head -1)
fi

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}❌ No se pudo encontrar/compilar el archivo JAR${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Usando JAR: $JAR_FILE${NC}"
echo -e "${GREEN}✓ Puerto: ${SERVER_PORT:-8080}${NC}"
echo -e "${GREEN}✓ Perfil: ${SPRING_PROFILE:-dev}${NC}"
echo ""

# Ejecutar la aplicación
echo -e "${BLUE}🚀 Iniciando aplicación...${NC}"
exec java $JAVA_OPTS \
    -Dspring.profiles.active=${SPRING_PROFILE:-dev} \
    -Dserver.port=${SERVER_PORT:-8080} \
    -Dlogging.file.name="$LOG_FILE" \
    -jar "$JAR_FILE"