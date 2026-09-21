# ==============================================================================
# ETAPA 1: Compilación de la aplicación Java con Maven
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 1. Cachear dependencias de Maven
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiar código fuente y compilar artefacto JAR
COPY backend/src ./src
RUN mvn clean package -Dmaven.test.skip=true

# ==============================================================================
# ETAPA 2: Imagen final ligera de ejecución (Eclipse Temurin 17 JRE Alpine)
# ==============================================================================
FROM eclipse-temurin:17-jre

LABEL maintainer="PaqRap Logistics <support@paqrap.com>"
LABEL description="Sistema Logístico PaqRap - Benchmark y API de Algoritmos Metaheurísticos"

WORKDIR /app

# Instalar bash para scripts de inicio

# Copiar el ejecutable compilado
COPY --from=builder /build/target/logistics-backend-*.jar /app/app.jar

# Copiar carpeta de datos y scripts de benchmark
COPY datos /app/datos
COPY entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["web"]
