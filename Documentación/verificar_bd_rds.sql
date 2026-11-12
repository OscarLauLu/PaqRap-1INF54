-- ============================================================
-- Verificación de la BD paqrap_operacion (RDS) — solo lectura
-- Ejecutar con DBeaver: selecciona UNA consulta y pulsa Ctrl+Enter.
-- ============================================================

-- 0. RESUMEN: filas por tabla (re-ejecútalo después de cada acción en la app)
SELECT 'pedidos'                  AS tabla, count(*) AS filas FROM pedidos
UNION ALL SELECT 'cliente',                count(*) FROM cliente
UNION ALL SELECT 'rutas',                  count(*) FROM rutas
UNION ALL SELECT 'paradas_ruta',           count(*) FROM paradas_ruta
UNION ALL SELECT 'bloqueos',               count(*) FROM bloqueos
UNION ALL SELECT 'averias',                count(*) FROM averias
UNION ALL SELECT 'alertas',                count(*) FROM alertas
UNION ALL SELECT 'movimiento_inventario',  count(*) FROM movimiento_inventario
UNION ALL SELECT 'mantenimiento_programado', count(*) FROM mantenimiento_programado
UNION ALL SELECT 'unidad_transporte',      count(*) FROM unidad_transporte
UNION ALL SELECT 'almacen',                count(*) FROM almacen
ORDER BY 1;

-- 1. PEDIDOS: los últimos registrados (manuales o por archivo)
SELECT codigo, cliente_id, cantidad_unidades, tipo_entrega, estado, nivel_criticidad,
       fecha_hora_registro, plazo_limite_entrega, unidad_asignada_id
FROM pedidos
ORDER BY id DESC
LIMIT 20;

-- 2. PEDIDOS por estado (cómo avanza la operación)
SELECT estado, count(*) FROM pedidos GROUP BY estado ORDER BY 2 DESC;

-- 3. BLOQUEOS cargados desde archivo
SELECT codigo, fecha_hora_inicio, fecha_hora_fin, activo, archivo_origen
FROM bloqueos
ORDER BY id DESC
LIMIT 20;

-- 4. FLOTA: estado de las 37 unidades (cambia cuando la operación asigna rutas)
SELECT estado_operativo, tipo_vehiculo_codigo, count(*) AS unidades
FROM unidad_transporte
GROUP BY estado_operativo, tipo_vehiculo_codigo
ORDER BY 1, 2;

-- 5. RUTAS planificadas con su unidad (FK rutas -> unidad_transporte)
SELECT r.codigo, r.unidad_id AS unidad, r.estado, r.distancia_total_km, r.costo_total,
       r.fecha_hora_generacion
FROM rutas r
ORDER BY r.id DESC
LIMIT 20;

-- 6. PEDIDO -> UNIDAD -> RUTA (comprueba las relaciones entre tablas)
SELECT p.codigo AS pedido, p.estado, p.unidad_asignada_id AS unidad,
       r.codigo AS ruta, pr.orden, pr.hora_estimada_llegada, pr.entregada
FROM paradas_ruta pr
JOIN pedidos p ON p.id = pr.pedido_id
JOIN rutas   r ON r.id = pr.ruta_id
ORDER BY r.id DESC, pr.orden
LIMIT 30;

-- 7. AVERÍAS por unidad
SELECT codigo, unidad_codigo, tipo, fecha_hora_evento, hora_reincorporacion, resuelta
FROM averias
ORDER BY id DESC
LIMIT 20;

-- 8. STOCK de almacenes y últimos movimientos
SELECT codigo, nombre, tipo_almacen, stock_actual, capacidad_maxima FROM almacen ORDER BY codigo;
SELECT codigo, almacen_codigo, tipo, cantidad, fecha_hora
FROM movimiento_inventario
ORDER BY fecha_hora DESC
LIMIT 20;

-- 9. MANTENIMIENTO: días bloqueados por unidad (FK a unidad_transporte)
SELECT unidad_codigo, count(*) AS dias_programados, min(fecha) AS primero, max(fecha) AS ultimo
FROM mantenimiento_programado
GROUP BY unidad_codigo
ORDER BY unidad_codigo
LIMIT 40;

-- 10. ALERTAS generadas
SELECT codigo, tipo, mensaje, atendida, fecha_hora_generacion
FROM alertas
ORDER BY id DESC
LIMIT 20;

-- 11. FKs hacia unidad_transporte (deben salir 5)
SELECT conrelid::regclass AS tabla, conname AS restriccion
FROM pg_constraint
WHERE contype = 'f' AND confrelid = 'unidad_transporte'::regclass
ORDER BY 1;
