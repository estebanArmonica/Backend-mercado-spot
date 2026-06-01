# Dockerfile
FROM openjdk:17-jdk-slim AS builder

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN useradd -m -u 1000 appuser

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p logs uploads backups && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Variables de entorno con configuración optimizada de GC
ENV JAVA_OPTS="-Xms1g -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseStringDeduplication \
    -XX:+UnlockExperimentalVMOptions \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1HeapRegionSize=16m \
    -XX:G1ReservePercent=20 \
    -XX:InitiatingHeapOccupancyPercent=45 \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -Xloggc:logs/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=logs/heapdump.hprof"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILE"]