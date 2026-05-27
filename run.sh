#!/bin/bash

# ============================================================================
# Script de lanzamiento del Backend de Mercado Spot ETL
# ============================================================================

# Configuración de colores para los output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No color

# Vaiables de configuración
APP_NAME="Backend-ETL-Mercado-Spot"
JAR_NAME="backend-etl-mercado-spot-1.0.0.jar"
PID_FILE="application.pid"
LOG_FILE="logs/application.log"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuración de Java
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-17-openjdk"}
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/heapdump.hprof"

# Variables de entorno
ENV_FILE="${APP_DIR}/.env"
if [ -f "$ENV_FILE" ]; then
    echo -e "${DEBUG}Cargando variables de entorno desde $ENV_FILE${NC}"
    set -a
    source "$ENV_FILE"
    set +a
fi

# Funciones de utilidad
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Función para verificar si la aplicación ya está corriendo
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1
}

# Funcion de verificación del puerto donde corre el proyecto
check_port() {
    local ports=${1:-8080}
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

# Funcion para crear directorios necesarios
setup_directories() {
    mkdir -p logs
    mkdir -p uploads
    mkdir -p backups
    log_info "Directories creados/verificados: logs, uploads, backups"
}

# Función para verificar dependencias
check_dependencies() {
    # Verificar Java
    if ! command -v java &> /dev/null; then
        if [ -d "$JAVA_HOME/bin" ]; then
            export PATH="$JAVA_HOME/bin:$PATH"
            log_info "Usando JAVA_HOME: $JAVA_HOME"
        else
            log_error "Java no está instalado o no se encuentra en PATH"
            log_error "Por favor, instala Java 17 o superior"
            exit 1
        fi
    fi
    
    # Verificar versión de Java
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        log_error "Se requiere Java 17 o superior. Versión actual: $JAVA_VERSION"
        exit 1
    fi
    
    log_info "Java versión: $(java -version 2>&1 | head -1)"
    
    # Verificar Maven (opcional, solo si se va a compilar)
    if [ "$1" == "build" ]; then
        if ! command -v mvn &> /dev/null; then
            log_error "Maven no está instalado. Por favor, instala Maven para compilar"
            exit 1
        fi
        log_info "Maven versión: $(mvn -version | head -1)"
    fi
}

# Función para compilar el proyecto
build_project() {
    log_info "Compilando el proyecto..."
    
    # Limpiar compilación anterior
    mvn clean
    
    # Compilar y empaquetar
    if mvn package -DskipTests; then
        log_info "Compilación exitosa"
        # Buscar el JAR generado
        JAR_FILE=$(find target -name "*.jar" -type f | head -1)
        if [ -n "$JAR_FILE" ]; then
            log_info "JAR generado: $JAR_FILE"
        fi
    else
        log_error "Error en la compilación"
        exit 1
    fi
}

# Función para iniciar la aplicación
start_app() {
    log_info "Iniciando $APP_NAME..."
    
    # Buscar el JAR más reciente
    JAR_FILE=$(find target -name "*.jar" -type f 2>/dev/null | head -1)
    
    # Si no encuentra JAR, intentar compilar
    if [ -z "$JAR_FILE" ]; then
        log_warn "No se encontró el archivo JAR. Compilando..."
        build_project
        JAR_FILE=$(find target -name "*.jar" -type f | head -1)
    fi
    
    if [ -z "$JAR_FILE" ]; then
        log_error "No se pudo encontrar/compilar el archivo JAR"
        exit 1
    fi
    
    log_info "Usando JAR: $JAR_FILE"
    
    # Iniciar la aplicación
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=${SPRING_PROFILE:-dev} \
        -Dlogging.file.name="$LOG_FILE" \
        -jar "$JAR_FILE" \
        >> "$LOG_FILE" 2>&1 &
    
    APP_PID=$!
    echo $APP_PID > "$PID_FILE"
    
    log_info "Aplicación iniciada con PID: $APP_PID"
    log_info "Logs: $LOG_FILE"
    
    # Esperar a que la aplicación esté lista
    sleep 5
    
    # Verificar si la aplicación está corriendo
    if is_running; then
        log_info "✅ Aplicación iniciada correctamente"
        return 0
    else
        log_error "❌ Error al iniciar la aplicación. Revisa los logs: $LOG_FILE"
        return 1
    fi
}

# Función para detener la aplicación
stop_app() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        log_info "Deteniendo aplicación (PID: $PID)..."
        
        # Intentar detener gracefulmente
        kill -15 "$PID"
        
        # Esperar hasta 30 segundos
        for i in {1..30}; do
            if ! ps -p "$PID" > /dev/null 2>&1; then
                log_info "Aplicación detenida correctamente"
                rm -f "$PID_FILE"
                return 0
            fi
            sleep 1
        done
        
        # Si no se detuvo, forzar
        log_warn "Forzando detención..."
        kill -9 "$PID" 2>/dev/null
        rm -f "$PID_FILE"
        log_info "Aplicación forzada a detenerse"
    else
        log_info "La aplicación no está corriendo"
    fi
}

# Función para reiniciar la aplicación
restart_app() {
    log_info "Reiniciando aplicación..."
    stop_app
    sleep 2
    start_app
}

# Función para verificar el estado
status_app() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        log_info "✅ Aplicación está corriendo (PID: $PID)"
        
        # Verificar puerto
        PORT=${SERVER_PORT:-8080}
        if check_port "$PORT"; then
            log_info "✅ Puerto $PORT está escuchando"
            # Hacer health check
            if command -v curl &> /dev/null; then
                HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT/actuator/health")
                if [ "$HEALTH_CHECK" = "200" ]; then
                    log_info "✅ Health check: OK"
                else
                    log_warn "Health check: HTTP $HEALTH_CHECK"
                fi
            fi
        else
            log_warn "⚠️ Puerto $PORT no está escuchando"
        fi
        
        # Mostrar logs recientes
        echo ""
        log_info "Últimas líneas del log:"
        tail -5 "$LOG_FILE" 2>/dev/null || log_warn "No se pudo leer el archivo de log"
    else
        log_error "❌ Aplicación no está corriendo"
    fi
}

# Función para mostrar logs
show_logs() {
    if [ -f "$LOG_FILE" ]; then
        if [ -n "$1" ]; then
            tail -f "$LOG_FILE"
        else
            tail -100 "$LOG_FILE"
        fi
    else
        log_error "Archivo de log no encontrado: $LOG_FILE"
    fi
}

# Función para mostrar ayuda
show_help() {
    echo ""
    echo "Uso: $0 {start|stop|restart|status|logs|build|help}"
    echo ""
    echo "Comandos:"
    echo "  start     - Inicia la aplicación"
    echo "  stop      - Detiene la aplicación"
    echo "  restart   - Reinicia la aplicación"
    echo "  status    - Muestra el estado de la aplicación"
    echo "  logs      - Muestra los logs (usar 'logs -f' para seguimiento)"
    echo "  build     - Compila el proyecto sin iniciar"
    echo "  help      - Muestra esta ayuda"
    echo ""
    echo "Variables de entorno:"
    echo "  SPRING_PROFILE - Perfil de Spring (dev|prod|test) - por defecto: dev"
    echo "  SERVER_PORT    - Puerto del servidor - por defecto: 8080"
    echo "  JAVA_OPTS      - Opciones adicionales para Java"
    echo ""
    echo "Ejemplos:"
    echo "  $0 start"
    echo "  $0 logs -f"
    echo "  SPRING_PROFILE=prod $0 start"
    echo ""
}

# Función principal
main() {
    cd "$APP_DIR" || exit 1
    
    setup_directories
    
    case "$1" in
        start)
            check_dependencies
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            check_dependencies
            restart_app
            ;;
        status)
            status_app
            ;;
        logs)
            shift
            show_logs "$1"
            ;;
        build)
            check_dependencies build
            build_project
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

# ejecutamos la función principal
main "$@"