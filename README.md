# PaqRap — Sistema Logístico

Proyecto del curso **Diseño y Desarrollo de Software (1INF54-0983)**.

PaqRap es un simulador de operaciones logísticas de última milla sobre una ciudad
modelada como una grilla de 70 × 50 nodos. El sistema planifica rutas de entrega para una
flota heterogénea (autos, motos y bicicletas) desde un almacén central y dos almacenes
intermedios, respetando plazos de entrega, capacidad de vehículos, stock de almacenes,
bloqueos viales temporales y averías mecánicas.

El núcleo del proyecto es la comparación de dos metaheurísticas de ruteo:

- **ACO** — Ant Colony Optimization (`planificacion/algoritmo/aco/`)
- **ALNS** — Adaptive Large Neighborhood Search (`planificacion/algoritmo/alns/`)

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5, Spring Data JPA, Lombok |
| Base de datos | H2 en memoria (desarrollo) · PostgreSQL 15 (perfil `prod`) |
| Documentación API | springdoc-openapi / Swagger UI |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, axios |
| Empaquetado | Maven, Docker, Docker Compose |

---

## Estructura del repositorio

```
.
├── backend/                  Aplicación Spring Boot (Maven)
│   └── src/main/java/com/paqrap/logistics/
│       ├── almacen/          Almacenes, stock y movimientos de inventario
│       ├── flota/            Unidades de transporte, conductores, turnos, averías
│       ├── pedidos/          Pedidos, clientes y semáforo de criticidad
│       ├── planificacion/    Planificador de rutas + algoritmos ACO y ALNS
│       ├── redvial/          Grilla, nodos, tramos y bloqueos viales
│       ├── simulacion/       Motor de simulación, reloj simulado, alertas
│       ├── simulation/       DTOs y motor de simulación orientado a la API
│       ├── common/           Enums, excepciones y utilidades compartidas
│       └── config/           Parámetros del sistema, CORS y OpenAPI
├── frontend/                 SPA React + Vite (ver frontend/README.md)
├── datos/                    Archivos planos de entrada (ventas, bloqueos, mantenimiento)
├── Dockerfile                Build multi-etapa: Maven → JRE Alpine
├── docker-compose.yml        Servicios: app, benchmark, db (perfil with-db)
├── entrypoint.sh             Selector de modo del contenedor (web | benchmark | sh)
├── docker-benchmark.sh       Helper que detecta docker compose vs docker
└── ejecutar_pruebas.sh       Atajo para lanzar el benchmark en Docker
```

---

## Requisitos

Para ejecución con Docker basta con **Docker** y **Docker Compose**.

Para ejecución local:

- JDK 17 y Maven 3.9+ (el repositorio no incluye Maven Wrapper)
- Node.js 18+ y `pnpm` (solo para el frontend)

---

## Puesta en marcha

### Opción A — Docker (recomendado)

```bash
# Levantar la API REST + Swagger UI en http://localhost:8080
docker compose up -d app

# Ver logs
docker compose logs -f app

# Detener
docker compose down
```

```bash
# Ejecutar el benchmark ACO vs ALNS y ver la tabla de resultados en la terminal
docker compose run --rm benchmark
```

La carpeta `datos/` se monta como volumen de solo lectura, así que se pueden editar los
archivos de entrada sin reconstruir la imagen. Detalles adicionales y variantes sin
Compose en [README_DOCKER.md](README_DOCKER.md).

> El frontend **no** está incluido en `docker-compose.yml`; se ejecuta localmente con `pnpm dev`.

### Opción B — Local

**Backend:**

```bash
cd backend
mvn spring-boot:run
```

La API queda en `http://localhost:8080` y Swagger UI en
`http://localhost:8080/swagger-ui.html`. Al arrancar, Hibernate crea el esquema H2 en
memoria (`ddl-auto: create-drop`) y `src/main/resources/data.sql` carga los datos
semilla: tipos de vehículo, almacenes y flota inicial.

**Frontend:**

```bash
cd frontend
pnpm install
pnpm dev
```

Disponible en `http://localhost:5173`. `vite.config.ts` incluye un proxy de `/api` hacia
`http://localhost:8080`, por lo que no hace falta configurar CORS en desarrollo. Si el
backend no responde, los hooks caen en los datos simulados de `src/api/mockData.ts`.

---

## API REST

Todos los endpoints están documentados de forma interactiva en
`http://localhost:8080/swagger-ui.html` (esquema OpenAPI en `/v3/api-docs`).

| Módulo | Base | Operaciones principales |
|---|---|---|
| Pedidos | `/api/pedidos` | Crear, listar, consultar, resumen, pendientes, actualizar, eliminar |
| Flota | `/api/flota` | Unidades, unidades disponibles, cambio de estado, registro de avería, asignación de conductor, configuración de tipo de vehículo |
| Almacenes | `/api/almacenes` | Listado, detalle, historial de movimientos, registro de carga, recarga diaria |
| Red vial | `/api/redvial` | Consulta de nodo, distancia, ruta mínima, bloqueos activos, carga de bloqueos desde archivo |
| Planificación | `/api/planificacion` | Ejecutar planificación, listar y consultar rutas, seleccionar algoritmo |
| Simulación | `/api/simulacion` | Configurar, iniciar, detener, reloj simulado, resultados, comparar algoritmos |
| Visualizador / Mapa | `/api/mapa` | Estado de operaciones, unidades, pedidos, incidencias, semáforo, alertas |

---

## Benchmark ACO vs ALNS

El mismo JAR sirve como servidor web y como ejecutor de benchmark: la bandera
`--benchmark` (o la variable de entorno `RUN_BENCHMARK=true`) activa el
`CommandLineRunner` de [LogisticsApplication.java](backend/src/main/java/com/paqrap/logistics/LogisticsApplication.java),
que carga los pedidos y bloqueos de `datos/`, corre ambos algoritmos y termina el proceso.

```bash
# Docker Compose
docker compose run --rm benchmark benchmark 202602 50     # mes 202602, 50 pedidos
docker compose run --rm benchmark benchmark 202609 80     # mes de alta demanda

# Local
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--benchmark --mes=202602 --pedidos=50"
```

Los argumentos posicionales se reconocen por su forma, en cualquier orden:

| Forma | Significado | Ejemplo |
|---|---|---|
| `NNNNNN` (6 dígitos) | Mes de ventas a cargar | `202601` |
| Ruta terminada en `.txt` | Archivo de ventas específico | `datos/ventas.v20260909/ventas.202603.txt` |
| `Nd`, `todos`, `all`, `mes` | Horizonte en días a simular | `7d` |
| `N` (entero) | Límite de pedidos | `50` |
| `diaN` / `inicioN` | Día de inicio dentro del mes | `dia10` |
| `averia` | Inyecta una avería en la simulación | `averia` |

También existen flags con nombre, que tienen precedencia sobre los posicionales:
`--mes`, `--pedidos`, `--dias`, `--inicio` / `--desde` / `--dia`, `--averia`, `--flota`.
Sin argumentos, el benchmark usa el mes `202601` con un horizonte de 5 días.

---

## Formatos de los archivos de `datos/`

**Ventas / pedidos** — `datos/ventas.v20260909/ventas.AAAAMM.txt`

```
##d##h##m:posX,posY,cIdCliente,cantidad,plazoHoras
01d01h30m:56,30,c4910,02,36
```

El año y el mes se infieren del nombre del archivo. Se acepta tanto `:` como `,` como
separador después de la marca de tiempo.

**Bloqueos viales** — `datos/bloqueos/bloqueo.AAMM.txt`

```
##d##h##m-##d##h##m:x1,y1,x2,y2,...,xn,yn
01d02h22m-01d04h42m:25,45,45,45,45,40
```

Rango de inicio y fin del bloqueo, seguido de la polilínea de nodos bloqueados.

**Mantenimiento preventivo** — `datos/mant.preventivo.09.10.txt`

```
AAAAMMDD:codigoUnidad
20260901:TA01
```

---

## Configuración

Los parámetros de negocio viven bajo la clave `logistics.config` de
[application.yml](backend/src/main/resources/application.yml) y se enlazan a
`config/SystemParameters.java`:

| Parámetro | Valor por defecto |
|---|---|
| `critical-slack-hours` | 4 |
| `service-time-minutes` | 60 |
| `grid-width` × `grid-height` | 70 × 50 |
| `warehouse-max-capacity` | 1000 |
| `warehouse-alert-threshold` | 90 % |
| `semaphore-green-hours` / `semaphore-amber-hours` | 12 / 4 |
| `planning-timeout-seconds` | 30 |
| `daily-restock-time` | 23:59:59 |

El bloque `logistics.config.vehicles` define velocidad, costo por km y capacidad de cada
tipo de vehículo. Ten en cuenta que los valores sembrados en `data.sql` no coinciden con
los de `application.yml` para velocidad; verifica cuál usa el flujo que estés tocando
antes de ajustar uno de los dos.

### Perfil `prod` (PostgreSQL)

```bash
docker compose --profile with-db up -d db
SPRING_PROFILES_ACTIVE=prod mvn -f backend/pom.xml spring-boot:run
```

Variables reconocidas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. En este
perfil `ddl-auto` es `validate`, por lo que el esquema debe existir previamente.

---

## Pruebas

```bash
cd backend
mvn test
```

- `AlgoritmosTest` — pruebas de los algoritmos de ruteo.
- `BenchmarkMetaheuristicasTest` — pruebas del comparador ACO/ALNS.

El frontend usa oxlint para análisis estático:

```bash
cd frontend
pnpm lint
```

---

## Documentación relacionada

- [README_DOCKER.md](README_DOCKER.md) — guía detallada de Docker y Docker Compose.
- [frontend/README.md](frontend/README.md) — arquitectura de custom hooks del frontend.
