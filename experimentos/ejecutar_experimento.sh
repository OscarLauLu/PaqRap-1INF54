#!/usr/bin/env bash
# ==============================================================================
# EJECUTOR DEL DISEÑO DE EXPERIMENTOS 5D: ACO vs. ALNS (PaqRap)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "================================================================================"
echo "          PAQRAP: EJECUTOR AUTOMATIZADO DE EXPERIMENTOS 5D (JAVA)               "
echo "================================================================================"

# Auto-detectar si docker requiere sudo
DOCKER_CMD="docker"
COMPOSE_CMD="docker-compose"
if [ "$EUID" -ne 0 ] && ! docker info >/dev/null 2>&1; then
    DOCKER_CMD="sudo docker"
    COMPOSE_CMD="sudo docker-compose"
fi

# Asegurar que el archivo de resultados exista para el montaje
touch "$SCRIPT_DIR/plantilla_resultados_5d.csv"

# 1. Probar con docker compose / docker-compose
if docker compose version >/dev/null 2>&1 || $DOCKER_CMD compose version >/dev/null 2>&1; then
    echo "-> Ejecutando con Docker Compose..."
    $DOCKER_CMD compose run --build --rm \
        -v "$SCRIPT_DIR/plantilla_resultados_5d.csv:/app/plantilla_resultados_5d.csv" \
        benchmark experimento "$@"

elif command -v docker-compose >/dev/null 2>&1; then
    echo "-> Ejecutando con docker-compose..."
    $COMPOSE_CMD run --build --rm \
        -v "$SCRIPT_DIR/plantilla_resultados_5d.csv:/app/plantilla_resultados_5d.csv" \
        benchmark experimento "$@"

# 2. Fallback con docker build + docker run
else
    echo "-> Compilando y ejecutando con Docker directo..."
    $DOCKER_CMD build -t paqrap .
    $DOCKER_CMD run --rm \
        -v "$SCRIPT_DIR/datos:/app/datos:ro" \
        -v "$SCRIPT_DIR/plantilla_resultados_5d.csv:/app/plantilla_resultados_5d.csv" \
        paqrap experimento "$@"
fi

echo ""
echo "================================================================================"
echo "  Resultados exportados a: $SCRIPT_DIR/plantilla_resultados_5d.csv"
echo "  Para ejecutar el análisis estadístico de Wilcoxon:"
echo "    python3 analisis_wilcoxon_5d.py"
echo "================================================================================"
