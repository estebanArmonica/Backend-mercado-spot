# usamos una imagen ligera de java
FROM openjdk:17-jdk-slim AS builder

# Instalamos Maven
RUN apt-get update && apt-get install -y maven
WORKDIR /app

# copiamos el pom.xml y descargamos todas las dependencias que tenga
COPY pom.xml .
RUN mvn dependency:go-offline

# copiamos el código fuente
COPY src ./src

# compilamos el proyecto
RUN mvn clean package -DskipTest
FROM openjdk:17-jdk-slim

# Instalamos curl para healtcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# creamos el usuario root
RUN useradd -m -u 1000 appuser

WORKDIR /app

# copiamos el JAR desde la etapa del builder
COPY --from=builder /app/target/*.jar app.jar

# creamos el directorio necesario 
RUN mkdir -p logs uploads backups && chown -R appuser:appuser /app

# cambiamos a usuario no root
USER appuser

# Exponemos el puerto del proyecto 
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Variables de entorno
ENV SPRING_PROFILE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILE"]