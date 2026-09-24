-- ============================================================
-- DATOS INICIALES DEL SISTEMA LOGÍSTICO (data.sql)
-- Remodelado a clave natural (codigo) según
-- Documentación/62.dis.base.datos.postgresql.v01.md
-- ============================================================

-- 1. TIPOS DE VEHÍCULO (RF-39, RF-40, RF-41, RF-46)
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TA', 'Auto', 24, 40.0, 8.00);
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TM', 'Moto', 8, 25.0, 6.00);
INSERT INTO tipo_vehiculo (codigo, nombre, capacidad_maxima, velocidad_promedio_kmh, costo_por_km)
VALUES ('TB', 'Bicicleta', 4, 12.0, 3.00);
-- 2. ALMACENES (RF-17, RF-18, RF-19, RF-20)
-- Almacén Central en (27, 14): stock_actual NULL = infinito (CHECK de la tabla "almacen")
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-CEN-01', 'CENTRAL', 'Almacén Central', 27, 14, NULL, NULL, NULL, NULL);

-- Almacén Intermedio Nor-Oeste en (12, 38), capacidad máxima 1,000 unidades
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-INT-01', 'INTERMEDIO', 'Almacén Intermedio Nor-Oeste', 12, 38, 1000, 1000, 90.0, '23:59:59');

-- Almacén Intermedio Este en (57, 27), capacidad máxima 1,000 unidades
INSERT INTO almacen (codigo, tipo_almacen, nombre, ubicacion_x, ubicacion_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES ('ALM-INT-02', 'INTERMEDIO', 'Almacén Intermedio Este', 57, 27, 1000, 1000, 90.0, '23:59:59');

-- 3. CATÁLOGO DE TURNOS (RF-43, RF-44) - 3 filas fijas
-- 1=Mañana 07:00-15:00, 2=Tarde 15:00-23:00, 3=Noche 23:00-07:00 (referenciados por id en FlotaController)
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (1, '07:00:00', '15:00:00');
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (2, '15:00:00', '23:00:00');
INSERT INTO turno (id, hora_inicio, hora_fin) VALUES (3, '23:00:00', '07:00:00');

-- 4. CONDUCTORES (RF-43, RF-44)
-- El turno ya no es un atributo fijo del conductor: se asigna por fecha en asignacion_turno (sección 6).
INSERT INTO conductor (codigo, nombre) VALUES ('CND-01', 'Juan Pérez');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-02', 'Carlos López');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-03', 'Miguel Gómez');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-04', 'David Silva');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-05', 'Jorge Ramos');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-06', 'Luis Torres');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-07', 'Andrés Mendoza');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-08', 'Pedro Castro');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-09', 'Roberto Vargas');
INSERT INTO conductor (codigo, nombre) VALUES ('CND-10', 'Víctor Morales');

-- 5. UNIDADES DE TRANSPORTE (RF-39, RF-42) - Flota completa del curso (37 unidades, inician en Almacén Central 27, 14)
-- Ya no llevan conductor_id ni fecha_ultimo_cambio_estado (fuera de alcance del esquema); dada_de_baja
-- reemplaza a "activo" con semántica invertida (false = en flota activa).
-- 10 Autos (tipo_vehiculo_codigo = TA, cap = 24)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA01', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA02', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA03', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA04', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA05', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA06', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA07', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA08', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA09', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA10', 'TA', 'DISPONIBLE', 27, 14, 0, false);

-- 15 Motos (tipo_vehiculo_codigo = TM, cap = 8)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM01', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM02', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM03', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM04', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM05', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM06', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM07', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM08', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM09', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM10', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM11', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM12', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM13', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM14', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM15', 'TM', 'DISPONIBLE', 27, 14, 0, false);

-- 12 Bicicletas (tipo_vehiculo_codigo = TB, cap = 4)
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB01', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB02', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB03', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB04', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB05', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB06', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB07', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB08', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB09', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB10', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB11', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB12', 'TB', 'DISPONIBLE', 27, 14, 0, false);

-- 6. ASIGNACIONES DE TURNO DE HOY (RF-43, RF-44)
-- Reconstruye los pares unidad/conductor que antes eran una FK fija en unidades_transporte,
-- ahora modelados como asignación por fecha (turno_id: 1=Mañana, 2=Tarde, 3=Noche).
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TA01', 'CND-01', 1, CURRENT_DATE, '10:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TA02', 'CND-02', 1, CURRENT_DATE, '10:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TA03', 'CND-03', 1, CURRENT_DATE, '10:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TA04', 'CND-04', 1, CURRENT_DATE, '10:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TM01', 'CND-05', 2, CURRENT_DATE, '18:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TM02', 'CND-06', 2, CURRENT_DATE, '18:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TM03', 'CND-07', 2, CURRENT_DATE, '18:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TB01', 'CND-08', 3, CURRENT_DATE, '02:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TB02', 'CND-09', 3, CURRENT_DATE, '02:00:00', 60);
INSERT INTO asignacion_turno (unidad_codigo, conductor_codigo, turno_id, fecha, hora_inicio_alimentacion, duracion_alimentacion_min) VALUES ('TB03', 'CND-10', 3, CURRENT_DATE, '02:00:00', 60);

-- 7. CLIENTES DE PRUEBA (RF-26)
-- id_cliente es ahora la PK (ya no hay un "id" numérico paralelo).
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-001', 'Corporación Alpha', 10, 12);
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-002', 'Distribuidora Beta', 45, 30);
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-003', 'Comercial Gamma', 20, 40);
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-004', 'Industrias Delta', 60, 15);
INSERT INTO cliente (id_cliente, nombre, ubicacion_entrega_x, ubicacion_entrega_y) VALUES ('CLI-005', 'Soluciones Epsilon', 32, 28);
