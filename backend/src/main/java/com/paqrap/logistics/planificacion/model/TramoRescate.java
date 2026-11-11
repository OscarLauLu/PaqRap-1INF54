package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tramo previo de una ruta de rescate: la unidad va desde donde estaba hasta el punto de la avería y
 * allí trasvasa la carga de la unidad averiada (30 min, FAQ / diagrama de clases: Trasvase).
 * Después la ruta sigue como cualquier otra, entregando desde el punto de la avería.
 * <p>
 * Equivale a un registro de la tabla {@code trasvase} del documento 62. Por ahora se mantiene en
 * memoria (la base H2 del perfil default tampoco sobrevive a un reinicio).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TramoRescate {

    /** Duración fija del trasvase (diagrama de clases, nota de Trasvase) */
    public static final int DURACION_TRASVASE_MIN = 30;

    private String codigoAveria;
    private String unidadAveriada;
    /** Desde dónde salió la unidad que rescata (un almacén o un punto de su regreso) */
    private Ubicacion partida;
    /** Punto de la avería, donde se hace el trasvase */
    private Ubicacion puntoTrasvase;
    private LocalDateTime salida;
    private LocalDateTime llegada;
    /** Fin del trasvase: desde aquí la carga ya va en la unidad que rescata */
    private LocalDateTime fin;
    private int cantidadProductos;
}
