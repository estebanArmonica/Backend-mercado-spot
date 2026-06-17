#!/bin/sh

echo "=== Iniciando Backend ETL Mercado Spot ==="
echo "Perfil: ${SPRING_PROFILE:-prod}"
echo "Puerto: ${PORT}"

# Crear .env desde variables de entorno
cat > .env << EOF
SUPERBASE_URL=${SUPERBASE_URL}
SUPERBASE_USER=${SUPERBASE_USER}
SUPERBASE_PASS=${SUPERBASE_PASS}
JWT_FIRMA=${JWT_FIRMA}
JWT_EXPIRATION=${JWT_EXPIRATION}
LOGGING_FILE_NAME=${LOGGING_FILE_NAME}
SERVER_PORT_PROD=${PORT:-8095}
EOF

# Ejecutar la aplicación
exec java -jar app.jar --spring.profiles.active=${SPRING_PROFILE:-prod} --server.port=${PORT:-8095}