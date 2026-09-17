#!/bin/sh
set -e

# Modo 1: Experimento formal 5D (ACO vs ALNS con réplicas pareadas)
if [ "$1" = "experimento" ] || [ "$1" = "exp" ]; then
    shift
    exec java -jar /app/app.jar --experimento "$@"

# Modo 2: Benchmark nativo Java (ACO vs ALNS)
elif [ "$1" = "benchmark" ] || [ "$1" = "bench" ]; then
    shift
    exec java -jar /app/app.jar --benchmark "$@"

# Modo 2: Shell interactivo
elif [ "$1" = "sh" ] || [ "$1" = "bash" ]; then
    exec "$@"

# Modo 4: Servidor Web Spring Boot por defecto
elif [ "$1" = "web" ]; then
    echo "================================================================================"
    echo "  INICIANDO SERVIDOR WEB SPRING BOOT: PAQRAP LOGISTICS API                    "
    echo "  Swagger UI disponible en: http://localhost:8080/swagger-ui.html              "
    echo "================================================================================"
    exec java -jar /app/app.jar

# Fallback: ejecutar java pasándole todos los argumentos
else
    exec java -jar /app/app.jar "$@"
fi
