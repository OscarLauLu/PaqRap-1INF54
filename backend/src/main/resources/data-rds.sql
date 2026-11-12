-- ============================================================
-- Seed idempotente para el perfil 'rds' (Postgres + Hibernate update).
-- Se ejecuta en cada arranque: ON CONFLICT / NOT EXISTS evitan duplicados.
-- Derivado de data.sql (que usa el perfil default con H2).
-- ============================================================
-- 1. TIPOS DE VEHÍCULO (RF-39, RF-40, RF-41, RF-46)
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TA', 'Auto', 24, 40.0, 8.00) ON CONFLICT DO NOTHING;
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TM', 'Moto', 8, 25.0, 6.00) ON CONFLICT DO NOTHING;
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TB', 'Bicicleta', 4, 12.0, 3.00) ON CONFLICT DO NOTHING;
-- 2. ALMACENES (RF-17, RF-18, RF-19, RF-20)
-- Almacén Central en (27, 14): stock_actual NULL = infinito (CHECK de la tabla "almacen")
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-CEN-01', 'CENTRAL', 'Almacén Central', 27, 14, NULL, NULL, NULL, NULL) ON CONFLICT DO NOTHING;

-- Almacén Intermedio Nor-Oeste en (12, 38), capacidad máxima 1,000 unidades
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-INT-01', 'INTERMEDIO', 'Almacén Intermedio Nor-Oeste', 12, 38, 1000, 1000, 90.0, '23:59:59') ON CONFLICT DO NOTHING;

-- Almacén Intermedio Este en (57, 27), capacidad máxima 1,000 unidades
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-INT-02', 'INTERMEDIO', 'Almacén Intermedio Este', 57, 27, 1000, 1000, 90.0, '23:59:59') ON CONFLICT DO NOTHING;

-- 3. CATÁLOGO DE TURNOS (RF-43, RF-44) - 3 filas fijas
-- 1=Mañana 07:00-15:00, 2=Tarde 15:00-23:00, 3=Noche 23:00-07:00 (referenciados por id en FlotaController)
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (1, '07:00:00', '15:00:00') ON CONFLICT DO NOTHING;
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (2, '15:00:00', '23:00:00') ON CONFLICT DO NOTHING;
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (3, '23:00:00', '07:00:00') ON CONFLICT DO NOTHING;

-- 4. CONDUCTORES (RF-43, RF-44)
-- El turno ya no es un atributo fijo del conductor: se asigna por fecha en asignacion_turno (sección 6).
INSERT INTO conductor (codigo, nombre) VALUES ('CND-01', 'Juan Pérez') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-02', 'Carlos López') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-03', 'Miguel Gómez') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-04', 'David Silva') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-05', 'Jorge Ramos') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-06', 'Luis Torres') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-07', 'Andrés Mendoza') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-08', 'Pedro Castro') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-09', 'Roberto Vargas') ON CONFLICT DO NOTHING;
INSERT INTO conductor (codigo, nombre) VALUES ('CND-10', 'Víctor Morales') ON CONFLICT DO NOTHING;

-- 5. UNIDADES DE TRANSPORTE (RF-39, RF-42) - Flota completa del curso (37 unidades, inician en Almacén Central 27, 14)
-- Ya no llevan conductor_id ni fecha_ultimo_cambio_estado (fuera de alcance del esquema); dada_de_baja
-- reemplaza a "activo" con semántica invertida (false = en flota activa).
-- 10 Autos (tipo_vehiculo_codigo = TA, cap = 24)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA01', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA02', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA03', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA04', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA05', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA06', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA07', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA08', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA09', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA10', 'TA', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;

-- 15 Motos (tipo_vehiculo_codigo = TM, cap = 8)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM01', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM02', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM03', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM04', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM05', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM06', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM07', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM08', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM09', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM10', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM11', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM12', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM13', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM14', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM15', 'TM', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;

-- 12 Bicicletas (tipo_vehiculo_codigo = TB, cap = 4)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB01', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB02', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB03', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB04', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB05', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB06', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB07', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB08', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB09', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB10', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB11', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB12', 'TB', 'DISPONIBLE', 27, 14, 0, false) ON CONFLICT DO NOTHING;

-- 6. ASIGNACIONES DE TURNO DE HOY (RF-43, RF-44)
-- Reconstruye los pares unidad/conductor que antes eran una FK fija en unidades_transporte,
-- ahora modelados como asignación por fecha (turno_id: 1=Mañana, 2=Tarde, 3=Noche).
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TA01', 'CND-01', 1, CURRENT_DATE, '10:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TA01' AND turno_id = 1 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TA02', 'CND-02', 1, CURRENT_DATE, '10:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TA02' AND turno_id = 1 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TA03', 'CND-03', 1, CURRENT_DATE, '10:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TA03' AND turno_id = 1 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TA04', 'CND-04', 1, CURRENT_DATE, '10:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TA04' AND turno_id = 1 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TM01', 'CND-05', 2, CURRENT_DATE, '18:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TM01' AND turno_id = 2 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TM02', 'CND-06', 2, CURRENT_DATE, '18:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TM02' AND turno_id = 2 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TM03', 'CND-07', 2, CURRENT_DATE, '18:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TM03' AND turno_id = 2 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TB01', 'CND-08', 3, CURRENT_DATE, '02:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TB01' AND turno_id = 3 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TB02', 'CND-09', 3, CURRENT_DATE, '02:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TB02' AND turno_id = 3 AND fecha = CURRENT_DATE);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) SELECT 'TB03', 'CND-10', 3, CURRENT_DATE, '02:00:00', 60 WHERE NOT EXISTS (SELECT 1 FROM asignacion_turno WHERE unidad_codigo = 'TB03' AND turno_id = 3 AND fecha = CURRENT_DATE);

-- 7. CLIENTES DE PRUEBA (RF-26)
-- id_cliente es ahora la PK (ya no hay un "id" numérico paralelo).
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-001', 'Corporación Alpha', 10, 12) ON CONFLICT DO NOTHING;
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-002', 'Distribuidora Beta', 45, 30) ON CONFLICT DO NOTHING;
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-003', 'Comercial Gamma', 20, 40) ON CONFLICT DO NOTHING;
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-004', 'Industrias Delta', 60, 15) ON CONFLICT DO NOTHING;
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-005', 'Soluciones Epsilon', 32, 28) ON CONFLICT DO NOTHING;

-- 8. FK pedido -> unidad de transporte (solo perfil rds).
-- Pedido.unidadAsignadaId es un String en la entidad; la FK se declara aquí y no en la entidad
-- para no afectar a las simulaciones (H2). Es seguro: el motor toma la flota de la tabla
-- unidad_transporte, así que nunca asigna una unidad inexistente. Idempotente en cada arranque.
ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS fk_pedido_unidad;
ALTER TABLE pedidos ADD CONSTRAINT fk_pedido_unidad FOREIGN KEY (unidad_asignada_id) REFERENCES unidad_transporte (codigo);
