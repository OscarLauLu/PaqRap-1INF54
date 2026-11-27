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

-- 5. UNIDADES DE TRANSPORTE (RF-39, RF-42) - Flota fijada por el docente (144 unidades, inician en Almacén Central 27, 14)
-- Autos 28 (cap 24), Motos 52 (cap 8), Bicicletas 64 (cap 4). Capacidad total: 28*24 + 52*8 + 64*4 = 1344.
-- Ya no llevan conductor_id ni fecha_ultimo_cambio_estado (fuera de alcance del esquema); dada_de_baja
-- reemplaza a "activo" con semántica invertida (false = en flota activa).
-- 28 Autos (tipo_vehiculo_codigo = TA, cap = 24)
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
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA11', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA12', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA13', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA14', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA15', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA16', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA17', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA18', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA19', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA20', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA21', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA22', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA23', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA24', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA25', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA26', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA27', 'TA', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TA28', 'TA', 'DISPONIBLE', 27, 14, 0, false);

-- 52 Motos (tipo_vehiculo_codigo = TM, cap = 8)
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
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM16', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM17', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM18', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM19', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM20', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM21', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM22', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM23', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM24', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM25', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM26', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM27', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM28', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM29', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM30', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM31', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM32', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM33', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM34', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM35', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM36', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM37', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM38', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM39', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM40', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM41', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM42', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM43', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM44', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM45', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM46', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM47', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM48', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM49', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM50', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM51', 'TM', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TM52', 'TM', 'DISPONIBLE', 27, 14, 0, false);

-- 64 Bicicletas (tipo_vehiculo_codigo = TB, cap = 4)
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
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB13', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB14', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB15', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB16', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB17', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB18', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB19', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB20', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB21', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB22', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB23', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB24', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB25', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB26', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB27', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB28', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB29', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB30', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB31', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB32', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB33', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB34', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB35', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB36', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB37', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB38', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB39', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB40', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB41', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB42', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB43', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB44', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB45', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB46', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB47', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB48', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB49', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB50', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB51', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB52', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB53', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB54', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB55', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB56', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB57', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB58', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB59', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB60', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB61', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB62', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB63', 'TB', 'DISPONIBLE', 27, 14, 0, false);
INSERT INTO unidad_transporte (codigo, tipo_vehiculo_codigo, estado_operativo, ubicacion_actual_x, ubicacion_actual_y, carga_actual, dada_de_baja) VALUES ('TB64', 'TB', 'DISPONIBLE', 27, 14, 0, false);

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