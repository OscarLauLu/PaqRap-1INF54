-- ============================================================
-- PaqRap — Esquema REAL (el que generó Hibernate en Postgres/AWS) portado a MySQL 8.4
-- Origen: DDL exportado de la BD en AWS. Misma estructura, tipos traducidos.
--
-- CÓMO SE USA (manual, una sola vez, con root desde Workbench):
--   1. Conectarse con root y seleccionar la BD:  USE paqrap_operacion;
--   2. Ejecutar este script COMPLETO sobre la BD VACÍA (usa CREATE TABLE sin IF NOT EXISTS:
--      si las tablas ya existen, falla en la primera — eso es intencional, no pisa datos).
--   3. Recién después arrancar el backend con el perfil "mysql". Hibernate (ddl-auto: update)
--      encuentra las tablas ya creadas y solo agregaría columnas que falten; data-mysql.sql
--      siembra tipos de vehículo, almacenes, turnos, conductores, flota y clientes.
--
-- NO está en db/migration a propósito: Flyway (perfil prod, Postgres) no debe tomarlo, y la
-- app no lo ejecuta sola. Revisado contra las 16 entidades JPA (nov-2026): mismas tablas,
-- columnas y valores de enum en los CHECK. Si se agrega un valor a un enum persistido
-- (EstadoPedido, TipoEntrega, etc.), hay que ampliar también el CHECK correspondiente aquí
-- y en la BD, o los INSERT con el valor nuevo fallarán.
-- ============================================================

-- Tabla alertas

CREATE TABLE alertas (
	id BIGINT AUTO_INCREMENT NOT NULL,
	atendida TINYINT(1) NOT NULL,
	codigo varchar(255) NULL,
	fecha_hora_generacion DATETIME(6) NULL,
	mensaje varchar(255) NULL,
	tipo varchar(255) NULL,
	CONSTRAINT alertas_pkey PRIMARY KEY (id),
	CONSTRAINT alertas_tipo_check CHECK (tipo IN ('UNIDAD_AVERIADA', 'OCUPACION_ALMACEN', 'PLAZO_INCUMPLIDO', 'VIA_BLOQUEADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla almacen

CREATE TABLE almacen (
	tipo_almacen varchar(31) NOT NULL,
	codigo varchar(255) NOT NULL,
	nombre varchar(255) NULL,
	stock_actual INT NULL,
	ubicacion_x INT NULL,
	ubicacion_y INT NULL,
	capacidad_maxima INT NULL,
	hora_recarga_diaria TIME(6) NULL,
	umbral_alerta_ocupacion DOUBLE NULL,
	CONSTRAINT almacen_pkey PRIMARY KEY (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla bloqueos

CREATE TABLE bloqueos (
	id BIGINT AUTO_INCREMENT NOT NULL,
	activo TINYINT(1) NOT NULL,
	archivo_origen varchar(255) NULL,
	codigo varchar(255) NOT NULL,
	coordenadas_nodos text NULL,
	fecha_hora_fin DATETIME(6) NOT NULL,
	fecha_hora_inicio DATETIME(6) NOT NULL,
	CONSTRAINT bloqueos_pkey PRIMARY KEY (id),
	CONSTRAINT uk_nb5r2wy97xcfbyhdm0w051106 UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla cliente

CREATE TABLE cliente (
	id_cliente varchar(255) NOT NULL,
	nombre varchar(255) NULL,
	ubicacion_entrega_x INT NULL,
	ubicacion_entrega_y INT NULL,
	CONSTRAINT cliente_pkey PRIMARY KEY (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla conductor

CREATE TABLE conductor (
	codigo varchar(255) NOT NULL,
	nombre varchar(255) NULL,
	CONSTRAINT conductor_pkey PRIMARY KEY (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla resultados_simulacion

CREATE TABLE resultados_simulacion (
	id BIGINT AUTO_INCREMENT NOT NULL,
	codigo varchar(255) NOT NULL,
	costo_total_auto DOUBLE NOT NULL,
	costo_total_bicicleta DOUBLE NOT NULL,
	costo_total_moto DOUBLE NOT NULL,
	costo_total_operacion DOUBLE NOT NULL,
	entregas_auto INT NOT NULL,
	entregas_bicicleta INT NOT NULL,
	entregas_moto INT NOT NULL,
	escenario varchar(255) NULL,
	fecha_hora_fin DATETIME(6) NULL,
	fecha_hora_inicio DATETIME(6) NULL,
	instante_colapso DATETIME(6) NULL,
	pedidos_entregados_en_plazo INT NOT NULL,
	pedidos_entregados_tarde INT NOT NULL,
	porcentaje_cumplimiento DOUBLE NOT NULL,
	session_id varchar(255) NULL,
	tiempo_ejecucion_real_segundos BIGINT NULL,
	total_averias_ocurridas INT NOT NULL,
	total_bloqueos_ocurridos INT NOT NULL,
	total_pedidos INT NOT NULL,
	volumen_pedidos_colapso INT NULL,
	CONSTRAINT resultados_simulacion_pkey PRIMARY KEY (id),
	CONSTRAINT uk_e799rwwkgneh099yoeteeda6k UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla tipo_vehiculo

CREATE TABLE tipo_vehiculo (
	codigo varchar(2) NOT NULL,
	actualizado_en DATETIME(6) NULL,
	capacidad_maxima INT NULL,
	costo_por_km DOUBLE NULL,
	nombre varchar(255) NULL,
	velocidad_promedio_kmh DOUBLE NULL,
	CONSTRAINT tipo_vehiculo_pkey PRIMARY KEY (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla turno

CREATE TABLE turno (
	id INT AUTO_INCREMENT NOT NULL,
	hora_fin TIME(6) NULL,
	hora_inicio TIME(6) NULL,
	CONSTRAINT turno_pkey PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla movimiento_inventario

CREATE TABLE movimiento_inventario (
	codigo varchar(255) NOT NULL,
	cantidad INT NOT NULL,
	codigo_ruta varchar(255) NULL,
	descripcion varchar(255) NULL,
	fecha_hora DATETIME(6) NULL,
	ruta_codigo varchar(255) NULL,
	tipo varchar(255) NULL,
	almacen_codigo varchar(255) NULL,
	CONSTRAINT movimiento_inventario_pkey PRIMARY KEY (codigo),
	CONSTRAINT movimiento_inventario_tipo_check CHECK (tipo IN ('CARGA', 'DESCARGA', 'RECARGA_DIARIA', 'LIBERACION_RESERVA')),
	CONSTRAINT fk19dg4xjoydxk8g2qpmhuw9p6i FOREIGN KEY (almacen_codigo) REFERENCES almacen(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla unidad_transporte

CREATE TABLE unidad_transporte (
	codigo varchar(255) NOT NULL,
	carga_actual INT NULL,
	dada_de_baja TINYINT(1) NULL,
	estado_operativo varchar(255) NOT NULL,
	ubicacion_actual_x INT NULL,
	ubicacion_actual_y INT NULL,
	tipo_vehiculo_codigo varchar(2) NOT NULL,
	CONSTRAINT unidad_transporte_estado_operativo_check CHECK (estado_operativo IN ('DISPONIBLE', 'EN_RUTA', 'EN_MANTENIMIENTO', 'AVERIADA')),
	CONSTRAINT unidad_transporte_pkey PRIMARY KEY (codigo),
	CONSTRAINT fk5780dss87hi0sbbictcfxuv9j FOREIGN KEY (tipo_vehiculo_codigo) REFERENCES tipo_vehiculo(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla asignacion_turno

CREATE TABLE asignacion_turno (
	id BIGINT AUTO_INCREMENT NOT NULL,
	duracion_alimentacion_min INT NOT NULL,
	fecha date NULL,
	hora_inicio_alimentacion TIME(6) NULL,
	ubicacion_relevo_x INT NULL,
	ubicacion_relevo_y INT NULL,
	conductor_codigo varchar(255) NULL,
	turno_id INT NULL,
	unidad_codigo varchar(255) NULL,
	CONSTRAINT asignacion_turno_pkey PRIMARY KEY (id),
	CONSTRAINT fk7ilthrb3kqm5jmb8k2tvq6y6d FOREIGN KEY (unidad_codigo) REFERENCES unidad_transporte(codigo),
	CONSTRAINT fkdo0etlkhiaf46x2p8bqmcx5p5 FOREIGN KEY (conductor_codigo) REFERENCES conductor(codigo),
	CONSTRAINT fkkrpejm7nrr4oy9h1lhiy404f6 FOREIGN KEY (turno_id) REFERENCES turno(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla averias

CREATE TABLE averias (
	id BIGINT AUTO_INCREMENT NOT NULL,
	codigo varchar(255) NULL,
	fecha_hora_evento DATETIME(6) NULL,
	hora_reincorporacion DATETIME(6) NULL,
	origen_manual TINYINT(1) NOT NULL,
	resuelta TINYINT(1) NOT NULL,
	tipo varchar(255) NULL,
	ubicacion_falla_x INT NULL,
	ubicacion_falla_y INT NULL,
	unidad_codigo varchar(255) NULL,
	CONSTRAINT averias_pkey PRIMARY KEY (id),
	CONSTRAINT averias_tipo_check CHECK (tipo IN ('TIPO_1', 'TIPO_2', 'TIPO_3')),
	CONSTRAINT fkrhladr2dvd3o3lhuftrgliavu FOREIGN KEY (unidad_codigo) REFERENCES unidad_transporte(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla mantenimiento_programado

CREATE TABLE mantenimiento_programado (
	codigo varchar(40) NOT NULL,
	fecha date NOT NULL,
	hora_fin TIME(6) NOT NULL,
	hora_inicio TIME(6) NOT NULL,
	unidad_codigo varchar(20) NOT NULL,
	CONSTRAINT mantenimiento_programado_pkey PRIMARY KEY (codigo),
	CONSTRAINT fk_mantenimiento_unidad FOREIGN KEY (unidad_codigo) REFERENCES unidad_transporte(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX ix_mantenimiento_unidad_fecha ON mantenimiento_programado (unidad_codigo, fecha);


-- Tabla pedidos

CREATE TABLE pedidos (
	id BIGINT AUTO_INCREMENT NOT NULL,
	cantidad_unidades INT NOT NULL,
	codigo varchar(255) NOT NULL,
	codigo_padre varchar(255) NULL,
	pos_x INT NULL,
	pos_y INT NULL,
	estado varchar(255) NOT NULL,
	fecha_hora_entrega DATETIME(6) NULL,
	fecha_hora_registro DATETIME(6) NOT NULL,
	nivel_criticidad varchar(255) NULL,
	plazo_limite_entrega DATETIME(6) NOT NULL,
	ruta_asignada_id BIGINT NULL,
	tipo_entrega varchar(255) NOT NULL,
	unidad_asignada_id varchar(255) NULL,
	cliente_id varchar(255) NULL,
	CONSTRAINT pedidos_estado_check CHECK (estado IN ('REGISTRADO', 'PLANIFICADO', 'EN_RUTA', 'ENTREGADO', 'CANCELADO')),
	CONSTRAINT pedidos_nivel_criticidad_check CHECK (nivel_criticidad IN ('VERDE', 'AMBAR', 'ROJO')),
	CONSTRAINT pedidos_pkey PRIMARY KEY (id),
	CONSTRAINT pedidos_tipo_entrega_check CHECK (tipo_entrega IN ('REGULAR_36H', 'PRIORIZADA_18H', 'PRIORIZADA_12H', 'PRIORIZADA_8H', 'PRIORIZADA_4H')),
	CONSTRAINT uk_opykg0avndf87lga45wx7l02v UNIQUE (codigo),
	CONSTRAINT fk6wgk5emlhdcthucnnmi4dpl33 FOREIGN KEY (cliente_id) REFERENCES cliente(id_cliente),
	CONSTRAINT fk_pedido_unidad FOREIGN KEY (unidad_asignada_id) REFERENCES unidad_transporte(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla rutas

CREATE TABLE rutas (
	id BIGINT AUTO_INCREMENT NOT NULL,
	codigo varchar(255) NOT NULL,
	costo_total DOUBLE NOT NULL,
	distancia_total_km DOUBLE NOT NULL,
	estado varchar(255) NULL,
	fecha_hora_generacion DATETIME(6) NULL,
	tiempo_estimado_min INT NOT NULL,
	almacen_id varchar(255) NULL,
	unidad_id varchar(255) NULL,
	CONSTRAINT rutas_estado_check CHECK (estado IN ('PLANIFICADA', 'EN_EJECUCION', 'REPLANIFICADA', 'COMPLETADA', 'CON_ALERTA')),
	CONSTRAINT rutas_pkey PRIMARY KEY (id),
	CONSTRAINT uk_pcuquq8a3mk7x9arptq5nxn2d UNIQUE (codigo),
	CONSTRAINT fk9wsaq3j4397dvxo4axr6axd44 FOREIGN KEY (almacen_id) REFERENCES almacen(codigo),
	CONSTRAINT fkjkmpr86fnyx4u8e4pgvlccrft FOREIGN KEY (unidad_id) REFERENCES unidad_transporte(codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Tabla paradas_ruta

CREATE TABLE paradas_ruta (
	id BIGINT AUTO_INCREMENT NOT NULL,
	entregada TINYINT(1) NOT NULL,
	hora_estimada_llegada DATETIME(6) NULL,
	orden INT NOT NULL,
	tiempo_servicio_min INT NOT NULL,
	pedido_id BIGINT NULL,
	ruta_id BIGINT NULL,
	CONSTRAINT paradas_ruta_pkey PRIMARY KEY (id),
	CONSTRAINT uk_2rw2863qtfhrpg2r3d28i1eg4 UNIQUE (pedido_id),
	CONSTRAINT fkgvm3ufah7o2miqfotce20y1ak FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
	CONSTRAINT fkj6lpcqia07kmmnq4huh2mpwbp FOREIGN KEY (ruta_id) REFERENCES rutas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
