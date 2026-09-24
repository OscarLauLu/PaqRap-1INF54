-- ============================================================
-- DATOS INICIALES DEL SISTEMA LOGÍSTICO (data.sql)
-- ============================================================

-- 1. TIPOS DE VEHÍCULO (RF-39, RF-40, RF-41, RF-46)
INSERT INTO tipos_vehiculo (id, nombre, capacidad_maxima, velocidad_promedio_km_h, costo_por_km)
VALUES (1, 'Auto', 24, 40.0, 8.00);

INSERT INTO tipos_vehiculo (id, nombre, capacidad_maxima, velocidad_promedio_km_h, costo_por_km)
VALUES (2, 'Moto', 8, 25.0, 6.00);

INSERT INTO tipos_vehiculo (id, nombre, capacidad_maxima, velocidad_promedio_km_h, costo_por_km)
VALUES (3, 'Bicicleta', 4, 12.0, 3.00);

-- 2. ALMACENES (RF-17, RF-18, RF-19, RF-20)
-- Almacén Central en (27, 14) según especificación oficial del 08-sept-2026
INSERT INTO almacenes (id, tipo_almacen, codigo, nombre, pos_x, pos_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES (1, 'CENTRAL', 'ALM-CEN-01', 'Almacén Central', 27, 14, 999999, NULL, NULL, NULL);

-- Almacén Intermedio Nor-Oeste en (12, 38), capacidad máxima 1,000 unidades
INSERT INTO almacenes (id, tipo_almacen, codigo, nombre, pos_x, pos_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES (2, 'INTERMEDIO', 'ALM-INT-01', 'Almacén Intermedio Nor-Oeste', 12, 38, 1000, 1000, 90.0, '23:59:59');

-- Almacén Intermedio Este en (57, 27), capacidad máxima 1,000 unidades
INSERT INTO almacenes (id, tipo_almacen, codigo, nombre, pos_x, pos_y, stock_actual, capacidad_maxima, umbral_alerta_ocupacion, hora_recarga_diaria)
VALUES (3, 'INTERMEDIO', 'ALM-INT-02', 'Almacén Intermedio Este', 57, 27, 1000, 1000, 90.0, '23:59:59');

-- 3. CONDUCTORES CON TURNOS (RF-43, RF-44)
-- Turno Mañana: 07:00 a 15:00
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (1, 'CND-01', 'Juan Pérez', '07:00:00', '15:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (2, 'CND-02', 'Carlos López', '07:00:00', '15:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (3, 'CND-03', 'Miguel Gómez', '07:00:00', '15:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (4, 'CND-04', 'David Silva', '07:00:00', '15:00:00', true);

-- Turno Tarde: 15:00 a 23:00
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (5, 'CND-05', 'Jorge Ramos', '15:00:00', '23:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (6, 'CND-06', 'Luis Torres', '15:00:00', '23:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (7, 'CND-07', 'Andrés Mendoza', '15:00:00', '23:00:00', true);

-- Turno Noche: 23:00 a 07:00
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (8, 'CND-08', 'Pedro Castro', '23:00:00', '07:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (9, 'CND-09', 'Roberto Vargas', '23:00:00', '07:00:00', true);
INSERT INTO conductores (id, codigo, nombre, hora_inicio, hora_fin, activo) VALUES (10, 'CND-10', 'Víctor Morales', '23:00:00', '07:00:00', true);

-- 4. UNIDADES DE TRANSPORTE (RF-39, RF-42) - Flota completa del curso (37 unidades, inician en Almacén Central 27, 14)
-- 10 Autos (id_tipo = 1, cap = 24)
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (1, 'TA01', 1, 'DISPONIBLE', 27, 14, 0, 1, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (2, 'TA02', 1, 'DISPONIBLE', 27, 14, 0, 2, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (3, 'TA03', 1, 'DISPONIBLE', 27, 14, 0, 3, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (4, 'TA04', 1, 'DISPONIBLE', 27, 14, 0, 4, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (5, 'TA05', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (6, 'TA06', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (7, 'TA07', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (8, 'TA08', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (9, 'TA09', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (10, 'TA10', 1, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);

-- 15 Motos (id_tipo = 2, cap = 8)
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (11, 'TM01', 2, 'DISPONIBLE', 27, 14, 0, 5, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (12, 'TM02', 2, 'DISPONIBLE', 27, 14, 0, 6, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (13, 'TM03', 2, 'DISPONIBLE', 27, 14, 0, 7, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (14, 'TM04', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (15, 'TM05', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (16, 'TM06', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (17, 'TM07', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (18, 'TM08', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (19, 'TM09', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (20, 'TM10', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (21, 'TM11', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (22, 'TM12', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (23, 'TM13', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (24, 'TM14', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (25, 'TM15', 2, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);

-- 12 Bicicletas (id_tipo = 3, cap = 4)
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (26, 'TB01', 3, 'DISPONIBLE', 27, 14, 0, 8, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (27, 'TB02', 3, 'DISPONIBLE', 27, 14, 0, 9, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (28, 'TB03', 3, 'DISPONIBLE', 27, 14, 0, 10, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (29, 'TB04', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (30, 'TB05', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (31, 'TB06', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (32, 'TB07', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (33, 'TB08', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (34, 'TB09', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (35, 'TB10', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (36, 'TB11', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);
INSERT INTO unidades_transporte (id, codigo, tipo_id, estado_operativo, pos_x, pos_y, carga_actual, conductor_id, activo, fecha_ultimo_cambio_estado)
VALUES (37, 'TB12', 3, 'DISPONIBLE', 27, 14, 0, NULL, true, CURRENT_TIMESTAMP);

-- 5. CLIENTES DE PRUEBA (RF-26)
INSERT INTO clientes (id, id_cliente, nombre, pos_x, pos_y) VALUES (1, 'CLI-001', 'Corporación Alpha', 10, 12);
INSERT INTO clientes (id, id_cliente, nombre, pos_x, pos_y) VALUES (2, 'CLI-002', 'Distribuidora Beta', 45, 30);
INSERT INTO clientes (id, id_cliente, nombre, pos_x, pos_y) VALUES (3, 'CLI-003', 'Comercial Gamma', 20, 40);
INSERT INTO clientes (id, id_cliente, nombre, pos_x, pos_y) VALUES (4, 'CLI-004', 'Industrias Delta', 60, 15);
INSERT INTO clientes (id, id_cliente, nombre, pos_x, pos_y) VALUES (5, 'CLI-005', 'Soluciones Epsilon', 32, 28);
