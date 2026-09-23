# Guía: Probar el Frontend con Docker

Esta guía explica cómo levantar la interfaz web (React + Vite) del sistema PaqRap usando Docker Compose, de forma independiente al backend o al benchmark.

> Nota: esta guía asume que ya tienes Docker Desktop instalado y funcionando (`docker --version` responde correctamente). Si no, sigue primero la guía de instalación de Docker.

---

## 1. Ubícate en la carpeta correcta

El archivo `docker-compose.yml` está en la raíz del repositorio (al mismo nivel que la carpeta `backend`, no dentro de ella). Abre una terminal ahí:

```bash
cd C:\Users\USUARIO\OneDrive\Escritorio\DP1_PaqRaq\PROYECTO-DE-DISE-O-Y-DESARROLLO-DE-SOFTWARE-1INF54-0983-
```

---

## 2. Levanta el servicio de frontend

```bash
docker compose up -d frontend
```

La primera vez va a tardar unos minutos: Docker construye la imagen (instala dependencias de Node/npm, compila el proyecto Vite) antes de arrancar el servidor de desarrollo.

---

## 3. Verifica que arrancó bien

```bash
docker compose logs -f frontend
```

Busca una línea similar a:

```
VITE vX.X.X  ready in XXX ms
➜  Local:   http://localhost:5173/
```

Presiona `Ctrl+C` para dejar de seguir los logs (esto no detiene el contenedor).

---

## 4. Abre la aplicación en el navegador

```
http://localhost:5173
```

Ahí deberías ver la interfaz visual del sistema (mapa, pedidos, panel de control, etc., según lo que tu compañero haya construido hasta ahora en el frontend).

---

## 5. ¿El frontend necesita el backend corriendo también?

Sí — el frontend por sí solo solo muestra la interfaz, pero para que cargue datos reales (almacenes, pedidos, rutas) necesita que el backend también esté levantado y respondiendo en el puerto 8080. Si vas a probar el frontend de forma funcional (no solo visualmente), levanta ambos:

```bash
docker compose up -d app frontend
```

Si el frontend muestra errores de conexión o pantallas vacías donde debería haber datos, revisa que:
- El backend esté realmente arriba (`docker compose logs -f app`, buscando `Started LogisticsApplication`).
- El frontend esté configurado para apuntar a `http://localhost:8080` como URL del API (esto normalmente vive en una variable de entorno o archivo de configuración del proyecto React — pregúntale a tu compañero si tiene dudas sobre esto).

---

## 6. Detener el frontend

```bash
docker compose down
```

Esto detiene y elimina los contenedores levantados (backend, frontend, o los que hayas iniciado). No borra tu código ni las imágenes ya construidas.

---

## Diferencia con las otras guías

| Guía | Para qué sirve | Puerto |
|---|---|---|
| `README_DOCKER.md` | Correr el benchmark de algoritmos (ACO vs. ALNS) | N/A (solo terminal) |
| `GUIA_DOCKER_BACKEND_API.md` | Probar los endpoints REST del backend vía Swagger | 8080 |
| `GUIA_DOCKER_FRONTEND.md` (esta) | Ver y usar la interfaz visual del sistema | 5173 |
