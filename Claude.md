# PaqRap — Backend (Java)

Este repositorio implementa el backend del sistema PaqRap para el curso 1INF54
(2026-2, equipo Eq9i / H0983). Este archivo es la memoria persistente del
proyecto: léelo al inicio de cualquier sesión antes de escribir código.

## Documentos de referencia (fuente de verdad — no los contradigas)

Coloca estos 3 archivos en `docs/` dentro de este repo y consúltalos antes de
diseñar cualquier entidad, tabla o módulo nuevo:

- `docs/61.dis.diagrama.clases.v03.md` — diagrama de clases de análisis (PlantUML). Es la fuente de verdad para nombres de clases, atributos y relaciones.
- `docs/62.dis.base.datos.postgresql.v01.md` — esquema PostgreSQL completo (DDL ya verificado corriendo contra Postgres 16). Úsalo tal cual para las entidades JPA/tablas; no inventes columnas nuevas sin actualizar antes este documento.
- `docs/63.dis.arquitectura.software.v01.docx` — arquitectura monolítica modular (vistas C4, decisiones AD-01 a AD-05, tácticas de calidad). Define capas, módulos y límites de responsabilidad.

Si necesitas cambiar el modelo de datos o la arquitectura, dilo explícitamente
y actualiza primero el documento correspondiente antes de tocar código — estos
3 documentos también se entregan como parte de la evaluación del curso, así
que deben mantenerse consistentes con el código.

## El caso (resumen — el detalle completo vive en los documentos del curso)

PaqRap vende un único producto "P". Entrega regular ≤36 h; priorizada en
4/8/12/18 h a elección del cliente. Política de negocio: 100% de pedidos
deben llegar en plazo (el sistema debe reportar explícitamente cuando un
pedido se vuelve infactible, no fallar en silencio).

- **Almacenes:** 1 central (stock infinito) + 2 intermedios (capacidad máx.
  1,000 u c/u, recarga instantánea diaria a las 23:59:59).
- **Flota:** autos (24 paquetes, 40 km/h, S/8.00/km), motos (8 paquetes,
  25 km/h, S/6.00/km), bicicletas (4 paquetes, 12 km/h, S/3.00/km). Carga
  despreciable; acondicionamiento de entrega al cliente = 1 h (no cuenta
  dentro del plazo comprometido).
- **Turnos:** cada 8 h (07:00, 15:00, 23:00); 1 h de alimentación obligatoria
  dentro de la jornada, ≥1 h antes/después del cambio de turno.
- **Red vial:** retícula ortogonal 70×50 km, nodo cada 1 km, todas las calles
  de doble sentido, sin diagonales ni curvas.
- **Averías:** T1 (2 h, permanece en el lugar), T2 (hasta fin del siguiente
  turno, 4 h en el lugar + traslado instantáneo a almacén central), T3 (≥2
  días, 4 h en el lugar + traslado instantáneo + reingresa en turno
  15:00-23:00). Una avería NO bloquea la vía.
- **Bloqueos:** solo planificados por la municipalidad (no hay fortuitos); un
  nodo bloqueado exige vuelta en U, no se puede atravesar.
- **Algoritmo:** el sistema **final** usa un único algoritmo, ACO
  (Optimización por Colonia de Hormigas) — así lo describen
  `61.dis.diagrama.clases.v03` (cambio DC-14) y `63.dis.arquitectura.software.v01`
  (decisión AD-05). **Importante — no es una contradicción con el código
  actual:** durante la experimentación numérica exigida por el curso (ISA v02,
  semanas 4-5), el backend SÍ contiene ambas implementaciones (ACO y ALNS)
  para poder compararlas; eso es esperado y correcto en esta etapa. Mantén el
  código de ambos algoritmos **aislado** dentro de `planificacion` (p. ej.
  `planificacion.algoritmos.AlgoritmoACO` y
  `planificacion.algoritmos.experimental.AlgoritmoALNS`, o un módulo Maven
  separado si se prefiere), para que cuando el equipo cierre la comparación
  y se quede con uno solo, sea borrar una carpeta y no desenredar código
  mezclado. No agregues un patrón Strategy/interfaz común permanente para
  alternar entre ambos en producción — eso es lo que sí se descartó a
  propósito (DC-14): el "seleccionable en caliente" no es un requisito, es
  solo una etapa transitoria de desarrollo.
- **Ciclo de planificación:** debe cerrar en ≤ 60 s.
- **Visualización:** mapa en tiempo real, multi-dispositivo, navegación por
  zonas (WebSocket, no polling — ver AD-03 en el documento de arquitectura).

### Los 3 escenarios de `MotorSimulacion` — semántica exacta de cada uno

- **`DIA_A_DIA`:** operación normal, sin duración fija.
- **`SIMULACION_5D`:** duración simulada fija de 5 días, con un límite de
  rendimiento real de 30-60 min de ejecución (medido en reloj de pared, no
  en tiempo simulado).
- **`COLAPSO_LOGISTICO`:** **sin duración fija.** El profesor entrega un
  dataset propio, diseñado deliberadamente para forzar el colapso. La
  definición de "colapso" es exacta y no admite umbral ni porcentaje:
  **el sistema colapsa en el instante en que UN SOLO pedido incumple su
  plazo límite de entrega** (`Pedido.plazoLimiteEntrega` pasa sin que el
  pedido esté `ENTREGADO`). En ese instante exacto, `MotorSimulacion` debe
  detener la simulación — no seguir corriendo ni "promediar" con pedidos
  que sí llegaron a tiempo. El objetivo del algoritmo (ACO) **no es evitar
  el colapso** (con datos adversariales, eventualmente va a colapsar) sino
  **aguantar la mayor cantidad de tiempo/carga posible antes de que ocurra**
  — esa es la métrica con la que el profesor evalúa qué tan bueno es el
  algoritmo. Por eso `ResultadoSimulacion.volumenPedidosColapso` importa:
  representa cuántos pedidos estaba manejando el sistema en el instante
  justo antes de romperse (más alto = el algoritmo aguantó más carga antes
  de fallar). `detectarColapso()` debe evaluarse en cada ciclo de
  planificación (o con más frecuencia) chequeando `calcularHolgura()` de
  todos los pedidos activos, no solo al final de la corrida.
  Antes de implementar esto, confirma con el equipo si el dataset del
  profesor entra por el mismo `CargadorArchivos` que ya carga
  bloqueos/averías/mantenimiento, o si necesita un formato/endpoint propio.

## Requisitos de alto nivel (trazabilidad — cada módulo debe poder señalar a cuál sirve)

| Req. | Descripción | Módulo |
|---|---|---|
| R1 | Planificar/replanificar rutas dinámicamente | Planificación de Rutas |
| R2 | Administrar inventario (central + 2 intermedios) | Almacenes e Inventario |
| R3 | Registrar pedidos por cliente | Gestión de Pedidos |
| R4 | Monitoreo y actualización de la flota | Gestión de Flota |
| R5 | Mapa interactivo en tiempo real, multi-dispositivo | Simulación, Monitoreo y Reportes (Visualizador) |
| R6 | Simular los 3 escenarios con KPIs configurables | Simulación, Monitoreo y Reportes (MotorSimulacion) |

## Arquitectura y stack

Confirmado con el "Documento de Estándares de Programación" del equipo — ya
no hay ambigüedad en estos puntos:

- **Estilo:** monolito modular en capas (Presentación → Aplicación/Dominio →
  Acceso a Datos), justificado en el documento de arquitectura (AD-01):
  equipo de 3 personas, alcance de un semestre, despliegue en equipos de
  laboratorio sin infraestructura adicional. **No propongas microservicios.**
  A nivel de código esto se aplica con **Package by Feature**: cada módulo de
  dominio tiene sus propias subcarpetas `controller/dto/model/repository/service`,
  en vez de una carpeta por capa a nivel raíz. Es la misma regla de
  dependencia (presentación → dominio → acceso a datos), aplicada dentro de
  cada paquete de feature.
- **Lenguaje:** Java 21 (Java SE).
- **Framework backend:** Spring Boot (REST + `spring-websocket` para el
  canal en tiempo real del Visualizador).
- **Persistencia:** PostgreSQL, mapeado con Spring Data JPA / Hibernate,
  siguiendo exactamente `docs/62.dis.base.datos.postgresql.v01.md`.
- **Frontend:** React 19 + TypeScript + Vite + Tailwind CSS 4. Consume el
  backend por REST (Axios/fetch) y WebSocket para el estado en tiempo real
  del mapa.
- **Paquete raíz:** `com.paqrap.logistics`, con 6 módulos de dominio + 2
  transversales:
  `pedidos`, `redvial`, `flota`, `almacen` (singular — así lo nombró el
  equipo), `planificacion` (incluye `AlgoritmoACO`, y temporalmente
  `AlgoritmoALNS` durante la experimentación — ver sección "El caso"),
  `simulacion` (incluye `MotorSimulacion` y `Visualizador`), más `common/` y
  `config/` para utilitarios y configuración transversal (CORS, seguridad).
  Ningún módulo accede directamente a las tablas de otro — solo a través de
  sus propias clases de dominio.
- **Publicador único de estado** (AD-03): un solo componente dentro de
  `simulacion` escribe y difunde el estado compartido por WebSocket; ningún
  otro hilo debe modificar ese estado directamente — esto es una decisión de
  arquitectura tomada específicamente para evitar condiciones de carrera
  entre el motor de simulación y las peticiones concurrentes de la interfaz
  (riesgo RSG-09 del CGR del equipo).
- **Control de versiones:** Git + GitHub, Git Flow simplificado
  (`main` / `develop` / `feature/*` / `fix/*`), commits estilo Conventional
  Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Convenciones de nomenclatura** (ver el documento de estándares del
  equipo para la tabla completa): clases/entidades en PascalCase, métodos y
  variables en camelCase, constantes en UPPER_SNAKE_CASE, controladores
  terminados en `Controller`, servicios en `Service`, repositorios en
  `Repository`, DTOs en `DTO`/`Request`/`Response`.

## Tarea urgente (hazla ANTES que el orden de implementación de abajo): reconciliar el esquema documentado con el código real

Ya existe código en este repo (entidades JPA + `data.sql` de prueba) que se
escribió **sin conocer** `docs/62.dis.base.datos.postgresql.v01.md`. Se
decidió con el equipo que **el documento manda** (sus datos vienen del caso
oficial confirmado); el código existente es el que se ajusta. No lo
reescribas todo de golpe — sigue este orden:

1. **Investiga primero, no cambies nada todavía.** Busca en todo el repo
   (especialmente el servicio/clase que arranca con el comando `benchmark`
   de `docker-compose.yml`, el que compara ACO vs. ALNS) qué código depende
   de las entidades actuales: `TipoVehiculo`, `Almacen`, `Conductor`,
   `UnidadTransporte`, `Cliente`. Repórtame la lista de archivos afectados
   antes de tocar nada — necesito ver el alcance real antes de aprobar los
   cambios.
2. **Corrige primero el bug de datos, independiente de todo lo demás:** en
   `data.sql`, las velocidades de Auto y Moto están invertidas y la de
   Bicicleta corrida. Deben ser Auto 40 km/h, Moto 25 km/h, Bicicleta
   12 km/h (capacidades y costos ya están bien: 24/S8, 8/S6, 4/S3).
3. **Reescribe las entidades JPA** para que mapeen exactamente al esquema de
   `docs/62.dis.base.datos.postgresql.v01.md`: nombres de tabla en singular
   (`tipo_vehiculo`, `almacen`, `conductor`, `unidad_transporte`, no
   `tipos_vehiculo`/`almacenes`/etc.), `codigo` como clave natural en las
   entidades que lo definen así en el documento (no un `id` autoincremental
   añadido aparte), `stock_actual = NULL` para el almacén central (no un
   sentinel como `999999` — hay un `CHECK` en el esquema que lo exige), y
   turno modelado como asignación por fecha (tablas `turno` +
   `asignacion_turno`) en vez de un `hora_inicio`/`hora_fin` fijo en
   `Conductor` y un `conductor_id` fijo en `UnidadTransporte`.
4. **Actualiza `data.sql`** con los nombres de tabla/columna nuevos (una vez
   corregidas las entidades del punto 3).
5. **Si el código de experimentación (benchmark ACO/ALNS) usa las entidades
   viejas,** actualízalo para que compile contra las nuevas — no lo dejes
   roto. Si el cambio es grande, dilo explícitamente en vez de reescribirlo
   silenciosamente.
6. Con esto verde en el perfil `default` (H2), recién ahí probamos el
   perfil `prod` contra Postgres real con las migraciones Flyway
   (`db/migration/V1__init_schema.sql`, `V2__seed_configuracion_semaforo.sql`,
   ya incluidas en este repo) — deberían aplicar sin fricción porque van a
   estar alineadas con las entidades nuevas.

**Decisión confirmada (22-sep-2026): se hace el remodelado completo**, con
clave natural (`codigo`) como PK real en las 5 entidades, tal como está en
`docs/62.dis.base.datos.postgresql.v01.md` — no la variante intermedia de
mantener `Long id` con `codigo` como columna única. Diagnóstico ya hecho por
Claude Code: toca ~24 archivos backend + 2 tests, y potencialmente 14
archivos de frontend (los DTOs pasan de `id: number` a `codigo: string`).

Orden de ejecución dentro de esta rama (de menor a mayor riesgo, cada capa
depende de la anterior — no saltes pasos):

1. `data.sql`: fix aislado del bug de velocidades (Auto 40, Moto 25,
   Bicicleta 12) — independiente de todo lo demás, hazlo primero.
2. **Modelo (11 archivos):** `almacen/model/{Almacen,AlmacenCentral,AlmacenIntermedio,MovimientoInventario}`,
   `flota/model/{Conductor,TipoVehiculo,UnidadTransporte,Turno,AsignacionTurno,Averia}`,
   `pedidos/model/Cliente`. Cambia `@Id`/`@GeneratedValue`/`@Column`/`@Table`
   al esquema documentado: PK `codigo` (String) en las 5 entidades de la
   tabla del diagnóstico; `Almacen.stockActual` de `int` a `Integer`
   (nullable); elimina el sentinel `999999` de `AlmacenCentral`; elimina
   `turno`/`activo` embebidos de `Conductor` y `conductorAsignado`/`activo`/
   `fechaUltimoCambioEstado` de `UnidadTransporte` (turno se consulta vía
   `asignacion_turno` por fecha); `Turno` pasa a catálogo real (3 filas);
   `AsignacionTurno` se remapea a tabla singular `asignacion_turno` con
   `turno_id` como FK y agrega `ubicacionRelevoX/Y`; `Cliente` colapsa
   `id`+`id_cliente` en un solo `idCliente` (String, PK).
3. **Repositorios (5 archivos):** `JpaRepository<X, Long>` → `JpaRepository<X, String>`
   en `AlmacenRepository`, `ConductorRepository`, `TipoVehiculoRepository`,
   `UnidadTransporteRepository`, `ClienteRepository`. Revisa cualquier
   método derivado que use `id`.
4. **Servicios (3 archivos):** `AlmacenService`, `FlotaService` (la lógica de
   asignación conductor-turno en líneas ~155-165 hay que reescribirla contra
   el modelo nuevo de `asignacion_turno` por fecha, no contra un
   `conductorAsignado` fijo), `PedidoService`. Cambia `findById(Long)` y
   `.getId()`/`.id(...)`.
5. **Controllers (4 archivos):** `AlmacenController`, `FlotaController`,
   `PedidoController`, `VisualizadorController`. `@PathVariable Long id` →
   `String codigo`. Esto cambia la firma pública de la API — anótalo
   explícitamente en el resumen final.
6. **DTOs (4 archivos):** `AlmacenDTO`, `MovimientoInventarioDTO`,
   `UnidadTransporteDTO`, `PedidoDTO`. Exponen `codigo` en vez de `id`.
7. **Algoritmos/benchmark (2 archivos):** `BenchmarkMetaheuristicas.java`,
   `alns/CapacidadAlmacenDestroyOperator.java`. No usan JPA/BD directamente,
   pero si construyen estas entidades o llaman `getStockActual()`/
   `getCapacidadMaxima()`, ajusta las llamadas a los constructores/campos
   nuevos.
8. **Tests (2 archivos):** `AlgoritmosTest.java`, `BenchmarkMetaheuristicasTest.java`
   — corrige las llamadas a constructores que ya no compilan.
9. **NO toques el frontend en esta misma tarea.** El cambio de `id: number`
   a `codigo: string` en los DTOs se propaga a ~14 archivos de frontend
   (`almacenesApi.ts`, `flotaApi.ts`, `mockData.ts`, `types/index.ts`, etc.)
   — trátalo como una tarea separada y explícita después de que el backend
   compile y los tests pasen, no en el mismo commit.

Al terminar cada bloque numerado, compila (`mvn compile` o `test`) antes de
seguir al siguiente — así el error queda acotado a la capa que acabas de
tocar, no mezclado con la siguiente.

Trabaja esto en una rama nueva (no directamente sobre `dev`), y muéstrame el
diff resumido por bloque antes de mezclarlo — es un cambio con riesgo real
de romper el código de otro integrante del equipo (el benchmark ACO/ALNS).

## Orden de implementación sugerido

No pidas "todo el backend" de una vez — se construye mejor en este orden,
porque cada capa depende de la anterior:

1. **Esquema y entidades base:** ejecutar `docs/62.dis.base.datos.postgresql.v01.md`
   contra Postgres, generar las entidades JPA/DTOs de `pedidos`, `flota`,
   `almacen` y `redvial` (sin lógica de negocio todavía).
2. **Gestión de Pedidos (R3):** CRUD de pedidos, cálculo de plazo límite y
   holgura, clasificación de criticidad (verde/ámbar/rojo).
3. **Red Vial y Flota (R4):** carga de bloqueos/averías/mantenimiento vía
   `CargadorArchivos`, cálculo de ruta mínima sobre la retícula, estado
   operativo de unidades.
4. **Almacenes e Inventario (R2):** stock, movimientos, recarga diaria.
5. **Planificación de Rutas con ACO (R1):** el ciclo de planificación
   completo — este es el componente más crítico en rendimiento (presupuesto
   de 60 s), constrúyelo con pruebas de tiempo desde el inicio.
6. **Simulación, Monitoreo y Reportes (R5, R6):** `MotorSimulacion`, los 3
   escenarios, el canal WebSocket del Visualizador y los KPIs. **Lee primero
   la sección "Los 3 escenarios de MotorSimulacion" más arriba** antes de
   programar `detectarColapso()` — el colapso se dispara con el primer
   pedido incumplido, no con un umbral ni un porcentaje.

## Reglas de trabajo

- Antes de crear una tabla, clase o endpoint nuevo que no esté en los 3
  documentos de referencia, dilo explícitamente — no lo agregues en
  silencio.
- Escribe pruebas unitarias para las reglas de negocio con números concretos
  del caso (plazos, capacidades, velocidades, costos) — no las inventes.
- Si una regla del caso no está clara en estos documentos, pregunta antes de
  asumir.
