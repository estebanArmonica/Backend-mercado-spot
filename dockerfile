# creamos el build
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar pom.xml y descargar dependencias (capa cacheada)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Empaquetar la aplicación (saltando tests para build más rápido)
RUN mvn clean package -DskipTests

# creamos el runtime
FROM eclipse-temurin:17-jre-alpine

# Instalar curl para healthchecks
RUN apk add --no-cache curl

# Crear usuario no root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Establecer el directorio de trabajo
WORKDIR /app

# Crear directorio para logs
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar a usuario no root
USER spring

# Exponer el puerto
EXPOSE 8095

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]