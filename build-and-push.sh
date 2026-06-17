#!/bin/bash

# =========================================================================
# Construccion de Docker (creacion, recosntruccion de imagen docker)
# También levantamiento de version actualizada en Docker Hub
# =========================================================================


GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Configuración
DOCKER_USERNAME="armonica21"
IMAGEN_NAME="etl-backend-mercado-spot"
REGISTRY="${DOCKER_USERNAME}/${IMAGEN_NAME}"

# Generar versión automática basada en fecha
VERSION=$(date +"%Y.%m.%d-%H%M%S")
# O usar versión manual
# VERSION="1.0.1"

echo -e "${BLUE}==============================================${NC}"
echo -e "${BLUE}   Construyendo y subiendo imagen Docker${NC}"
echo -e "${BLUE}==============================================${NC}"

# Verificar Docker
if ! docker version &> /dev/null; then
    echo -e "${RED}❌ Docker no está corriendo${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker está corriendo${NC}"
echo -e "${BLUE}📦 Versión: ${VERSION}${NC}"

# Construir imagen
echo -e "${BLUE}📦 Construyendo imagen...${NC}"
docker build -f dockerfile.prod \
  -t ${REGISTRY}:${VERSION} \
  -t ${REGISTRY}:latest \
  .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al construir la imagen${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Imagen construida correctamente${NC}"

# Subir a Docker Hub
echo -e "${BLUE}📤 Subiendo a Docker Hub...${NC}"

echo -e "${YELLOW}📤 Subiendo versión: ${VERSION}${NC}"
docker push ${REGISTRY}:${VERSION}

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al subir versión ${VERSION}${NC}"
    exit 1
fi

echo -e "${YELLOW}📤 Subiendo versión: latest${NC}"
docker push ${REGISTRY}:latest

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error al subir versión latest${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Imagen subida correctamente${NC}"
echo ""

# Mostrar información
echo -e "${BLUE}📋 Información de la imagen:${NC}"
echo -e "  Nombre: ${GREEN}${REGISTRY}${NC}"
echo -e "  Versiones: ${GREEN}${VERSION}${NC}, ${GREEN}latest${NC}"
echo ""

echo -e "${BLUE}📦 Tamaño de la imagen:${NC}"
docker images ${REGISTRY} --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}"

echo ""
echo -e "${BLUE}🚀 Para probar localmente:${NC}"
echo -e "  docker run -d -p 8095:8080 --env-file .env ${REGISTRY}:latest"

echo ""
echo -e "${BLUE}🚀 Para desplegar en Cloud Run:${NC}"
echo -e "  gcloud run deploy etl-backend-mercado-spot \\"
echo -e "    --image docker.io/${REGISTRY}:latest \\"
echo -e "    --platform managed \\"
echo -e "    --region us-central1 \\"
echo -e "    --memory 1Gi \\"
echo -e "    --cpu 1 \\"
echo -e "    --timeout 3600 \\"
echo -e "    --port 8080 \\"
echo -e "    --allow-unauthenticated \\"
echo -e "    --set-env-vars \"SPRING_PROFILE=prod,\\\" \\"
echo -e "    --set-env-vars \"JWT_EXPIRATION=86400000,\\\" \\"
echo -e "    --set-env-vars \"LOGGING_FILE_NAME=logs/application.log\""

echo ""
echo -e "${GREEN}✅ Proceso completado exitosamente!${NC}"