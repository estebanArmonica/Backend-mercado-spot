#!/bin/bash

# ==========================================================
# Script para construir y subir imagen a Docker Hub
# ==========================================================

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuración
IMAGEN_NAME="etl-backend-mercado-spot"
DOCKER_USERNAME="armonica21"
VERSION="1.0.0"
REGISTRY="${DOCKER_USERNAME}/${IMAGEN_NAME}"

echo -e "${BLUE}==============================================${NC}"
echo -e "${BLUE}         Construyendo y subiendo imagen Docker${NC}"
echo -e "${BLUE}==============================================${NC}"

# Verificar que Docker está corriendo
if ! docker version &> /dev/null; then
    echo -e "${RED}❌ Docker no está corriendo. Inicia Docker Desktop primero.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker está corriendo${NC}"

# Verificar que el Dockerfile existe
if [ ! -f "dockerfile.prod" ]; then
    echo -e "${RED}❌ No se encontró dockerfile.prod${NC}"
    exit 1
fi

# Construir la imagen
echo -e "${BLUE}📦 Construyendo imagen Docker...${NC}"
docker build -f dockerfile.prod -t ${REGISTRY}:${VERSION} -t ${REGISTRY}:latest .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al construir la imagen${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Imagen construida correctamente${NC}"

# Subir la imagen a Docker Hub
echo -e "${BLUE}📤 Subiendo imagen a Docker Hub...${NC}"
echo -e "${YELLOW}Subiendo versión: ${VERSION}${NC}"
docker push ${REGISTRY}:${VERSION}

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al subir la versión ${VERSION}${NC}"
    exit 1
fi

echo -e "${YELLOW}Subiendo versión: latest${NC}"
docker push ${REGISTRY}:latest

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al subir la versión latest${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Imagen subida correctamente a Docker Hub${NC}"
echo ""

# Mostrar información
echo -e "${BLUE}📋 Información de la imagen:${NC}"
echo -e "  Nombre: ${GREEN}${REGISTRY}${NC}"
echo -e "  Versiones: ${GREEN}${VERSION}${NC}, ${GREEN}latest${NC}"
echo ""

echo -e "${BLUE}📦 Tamaño de la imagen:${NC}"
docker images ${REGISTRY} --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}"

echo ""
echo -e "${GREEN}✅ Proceso completado exitosamente!${NC}"