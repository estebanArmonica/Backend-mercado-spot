# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

RUN adduser -D -u 1000 appuser

WORKDIR /app

# Copiar el JAR y el archivo .env
COPY --from=builder /app/target/*.jar app.jar
COPY .env .env

RUN mkdir -p logs uploads backups && chown -R appuser:appuser /app

USER appuser

EXPOSE 8095

# Ejecutar con variables de entorno y .env
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=prod", "--server.port=8095"]