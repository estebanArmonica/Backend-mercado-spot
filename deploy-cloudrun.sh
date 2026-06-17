#!/bin/bash

# ========================================================
# Script para desplegar en Cloud Run
# ========================================================

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' 

# Configuración
PROJECT_ID=""
SERVICE_NAME="etl-backend-mercado-spot"
REGION="us-central1"
IMAGE="docker.io/armonica21/etl-backend-mercado-spot:latest"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}     Desplegando en Cloud Run${NC}"
echo -e "${BLUE}============================================${NC}"

# Verificamos que gcloud está instalado
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED} gcloud no está instalado${NC}"
    echo -e "${YELLOW}Instala Google Cloud SDK: https://cloud.google.com/sdk/docs/install${NC}"
    exit 1
fi

# Verificación automatica
echo -e "${BLUE} Verificando autenticación en GCP...${NC}"
if ! gcloud auth print-access-token &> /dev/null; then
    echo -e "${YELLOW} No estás autenticado en GCP${NC}"
    echo -e "${BLUE} Inicia sesión ejecutando:${NC}"
    echo -e "   gcloud auth login"
    exit 1
fi

echo -e "${GREEN} Autenticación verificada${NC}"

# Configuramos el proyecto
echo -e "${BLUE} Configurando proyecto: ${PROJECT_ID}${NC}"
gcloud config set project ${PROJECT_ID}

# Desplegamos en Cloud Run
echo -e "${BLUE} Desplegando en Cloud Run...${NC}"
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE} \
  --platform managed \
  --region ${REGION} \
  --memory 1Gi \
  --cpu 1 \
  --timeout 3600 \
  --port 8080 \
  --allow-unauthenticated \
  --set-env-vars "^#^SPRING_PROFILE=prod,\
JWT_EXPIRATION=86400000,\
LOGGING_FILE_NAME=logs/application.log"

if [ $? -ne 0 ]; then
    echo -e "${RED} Error al desplegar en Cloud Run${NC}"
    exit 1
fi

echo -e "${GREEN} Despliegue completado exitosamente!${NC}"

# Mostramos la URL del servicio
echo ""
echo -e "${BLUE} URL del servicio:${NC}"
gcloud run services describe ${SERVICE_NAME} --region ${REGION} --format="value(status.url)"

echo ""
echo -e "${BLUE} Ver logs:${NC}"
echo -e "  gcloud run logs read ${SERVICE_NAME} --region ${REGION}"