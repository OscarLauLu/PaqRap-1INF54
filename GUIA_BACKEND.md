# Guía: Probar el Backend/API con Docker (Swagger UI)

Esta guía explica cómo levantar el backend (API REST) con Docker y probar sus endpoints manualmente, sin necesidad de Postman ni escribir código.

> Nota: esta guía asume que ya tienes Docker Desktop instalado y funcionando, y que el `Dockerfile` en la raíz del repo ya usa Java 21 (`maven:3.9.16-eclipse-temurin-21` / `eclipse-temurin:21-jre`), acorde al `pom.xml` actual.

---

## 1. ¿Qué es Swagger UI?

Es una interfaz web que se genera automáticamente a partir del código del backend (gracias a la librería `springdoc-openapi`, incluida en el `pom.xml`). Lista todos los endpoints disponibles (`/api/almacenes`, `/api/pedidos`, etc.), agrupados por controlador, y te deja **ejecutarlos directamente desde el navegador** — sin necesitar Postman, curl, ni escribir una sola línea de código. Es la forma más rápida de verificar manualmente que un endpoint funciona como se espera.

---

## 2. Ubícate en la carpeta correcta

El `docker-compose.yml` está en la raíz del repositorio:

```bash
cd C:\Users\USUARIO\OneDrive\Escritorio\DP1_PaqRaq\PROYECTO-DE-DISE-O-Y-DESARROLLO-DE-SOFTWARE-1INF54-0983-
```

---

## 3. Levanta el backend

```bash
docker compose up -d app
```

La primera vez, Docker va a: descargar la imagen base de Maven+JDK 21, copiar tu código, compilarlo (`mvn clean package`), y luego arrancar la aplicación Spring Boot dentro de un contenedor más liviano. Esto puede tardar varios minutos la primera vez; las siguientes veces será mucho más rápido si no cambiaste el código (Docker reutiliza capas ya construidas).

---

## 4. Verifica que arrancó sin errores

```bash
docker compose logs -f app
```

Busca la línea de confirmación:

```
Started LogisticsApplication in X seconds
```

Si en cambio ves un error (por ejemplo, un `BeanCreationException` o un `NullPointerException`), cópialo completo — es el mismo tipo de diagnóstico que hicimos antes con `mvn spring-boot:run`.

Presiona `Ctrl+C` para dejar de seguir los logs (el contenedor sigue corriendo en segundo plano).

---

## 5. Abre Swagger UI

```
http://localhost:8080/swagger-ui.html
```

Vas a ver una lista de secciones (una por cada controlador: Almacenes, Pedidos, Clientes, Vehículos, etc.), cada una desplegable con sus endpoints.

---

## 6. Cómo probar un endpoint

1. Haz clic en el endpoint que quieras probar (por ejemplo, `GET /api/almacenes`).
2. Haz clic en el botón **"Try it out"**.
3. Si el endpoint pide parámetros (como un `id` en la URL, o filtros de búsqueda), complétalos en los campos que aparecen.
4. Haz clic en **"Execute"**.
5. Revisa la sección **"Server response"**:
   - **Código `200`**: el endpoint respondió correctamente. Verás el cuerpo de la respuesta (JSON) debajo.
   - **Código `404`**: no encontró el recurso pedido (por ejemplo, un ID que no existe en la base — esto puede ser esperado si la base está vacía).
   - **Código `500`**: hay un error real en el backend. Revisa los logs del contenedor (`docker compose logs -f app`) para ver el stack trace completo y diagnosticar la causa.

---

## 7. Endpoints recomendados para una primera verificación

Si acabas de levantar el sistema y quieres confirmar rápido que todo funciona, prueba en este orden (los que no requieren datos previos):

- `GET /api/almacenes` — lista almacenes con su ocupación.
- `GET /api/clientes` (si existe) — lista clientes de ejemplo.
- `GET /api/pedidos` (sin filtros, o con un filtro que sepas que no debería fallar) — lista pedidos.

Si alguno de estos da `500`, es una señal de un bug real que hay que reportar (con el stack trace) antes de dar por buena esa versión del backend.

---

## 8. Detener el backend

```bash
docker compose down
```

---

## Diferencia con las otras guías

| Guía | Para qué sirve | Puerto |
|---|---|---|
| `README_DOCKER.md` | Correr el benchmark de algoritmos (ACO vs. ALNS) | N/A (solo terminal) |
| `GUIA_DOCKER_BACKEND_API.md` (esta) | Probar los endpoints REST del backend vía Swagger | 8080 |
| `GUIA_DOCKER_FRONTEND.md` | Ver y usar la interfaz visual del sistema | 5173 |
