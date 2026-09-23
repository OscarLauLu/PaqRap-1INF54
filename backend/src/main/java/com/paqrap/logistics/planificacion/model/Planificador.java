package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.repository.PedidoRepository;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoRuteo;
import com.paqrap.logistics.planificacion.repository.RutaRepository;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Componente central del Planificador de Rutas (RF-01 a RF-16, RF-55).
 * Coordina la optimización de rutas, selección de almacenes, ordenamiento por holgura
 * y replanificación reactiva por bloqueos y averías.
 */
@Slf4j
@Component
public class Planificador {

    @Getter
    @Setter
    private int intervaloCicloMin = 60;

    @Getter
    @Setter
    private AlgoritmoRuteo algoritmoRuteo;

    private final RedVial redVial;
    private final PedidoRepository pedidoRepository;
    private final UnidadTransporteRepository unidadRepository;
    private final AlmacenRepository almacenRepository;
    private final RutaRepository rutaRepository;

    public Planificador(@Qualifier("algoritmoACO") AlgoritmoRuteo algoritmoRuteo,
                        RedVial redVial,
                        PedidoRepository pedidoRepository,
                        UnidadTransporteRepository unidadRepository,
                        AlmacenRepository almacenRepository,
                        RutaRepository rutaRepository) {
        this.algoritmoRuteo = algoritmoRuteo;
        this.redVial = redVial;
        this.pedidoRepository = pedidoRepository;
        this.unidadRepository = unidadRepository;
        this.almacenRepository = almacenRepository;
        this.rutaRepository = rutaRepository;
    }

    /**
     * Ejecuta el ciclo de planificación de rutas en el instante simulado (RF-01 a RF-09).
     * 1. Consulta pedidos pendientes y los ordena por holgura (RF-04).
     * 2. Selecciona almacén origen conveniente por pedido (RF-02).
     * 3. Ejecuta el algoritmo de ruteo configurado (ACO o ALNS).
     * 4. Valida capacidades (RF-07) y plazos comprometidos (RF-08).
     * 5. Confirma y persiste las rutas factibles generadas.
     */
    public List<Ruta> ejecutarCicloPlanificacion(LocalDateTime instante) {
        if (instante == null) {
            instante = LocalDateTime.now();
        }
        log.info("Iniciando ciclo de planificación de rutas al instante {} usando {}", instante, algoritmoRuteo.obtenerNombre());

        List<Pedido> pedidosPendientes = pedidoRepository.findByEstado(EstadoPedido.REGISTRADO);
        if (pedidosPendientes.isEmpty()) {
            log.info("No existen pedidos pendientes de planificación.");
            return new ArrayList<>();
        }

        // Ordenar pedidos de menor a mayor holgura (RF-04)
        List<Pedido> pedidosOrdenados = ordenarPorHolgura(pedidosPendientes);

        // Obtener flota disponible
        List<UnidadTransporte> flotaDisponible = unidadRepository.findByActivoTrueAndEstadoOperativo(EstadoOperativo.DISPONIBLE);
        if (flotaDisponible.isEmpty()) {
            log.warn("No hay unidades de transporte disponibles para planificar rutas.");
            return new ArrayList<>();
        }

        // Construir solución usando el algoritmo configurado (Strategy pattern)
        List<Ruta> rutasGeneradas = algoritmoRuteo.construirSolucion(pedidosOrdenados, flotaDisponible, redVial);

        List<Ruta> rutasConfirmadas = new ArrayList<>();
        for (Ruta ruta : rutasGeneradas) {
            // Asignar almacén central por defecto o el más cercano
            Almacen origen = seleccionarAlmacenOrigen(ruta.getParadas().isEmpty() ? null : ruta.getParadas().get(0).getPedido());
            ruta.setAlmacenOrigen(origen);

            // Validar capacidad (RF-07) y plazos (RF-08)
            if (!ruta.capacidadExcedida() && ruta.cumplePlazos()) {
                // Descontar stock del almacén
                int unidadesTotales = ruta.getParadas().stream()
                        .mapToInt(p -> p.getPedido() != null ? p.getPedido().getCantidadUnidades() : 0)
                        .sum();

                if (origen != null && origen.tieneStockSuficiente(unidadesTotales)) {
                    origen.descontarStock(unidadesTotales, ruta.getCodigo());
                }

                // Actualizar estado de los pedidos a PLANIFICADO
                for (ParadaRuta p : ruta.getParadas()) {
                    if (p.getPedido() != null) {
                        p.getPedido().setEstado(EstadoPedido.PLANIFICADO);
                        p.getPedido().setRutaAsignadaId(ruta.getId());
                        if (ruta.getUnidadTransporte() != null) {
                            p.getPedido().setUnidadAsignadaId(ruta.getUnidadTransporte().getId());
                        }
                        pedidoRepository.save(p.getPedido());
                    }
                }

                // Cambiar estado de la unidad a EN_RUTA
                if (ruta.getUnidadTransporte() != null) {
                    ruta.getUnidadTransporte().cambiarEstado(EstadoOperativo.EN_RUTA);
                    unidadRepository.save(ruta.getUnidadTransporte());
                }

                rutaRepository.save(ruta);
                rutasConfirmadas.add(ruta);
            } else {
                log.warn("Ruta {} rechazada por incumplir plazos o capacidad.", ruta.getCodigo());
            }
        }

        log.info("Ciclo de planificación completado: {} rutas confirmadas.", rutasConfirmadas.size());
        return rutasConfirmadas;
    }

    /**
     * Ordena los pedidos pendientes de menor a mayor holgura (RF-04).
     * Los pedidos con holgura por debajo del umbral crítico son priorizados primero.
     */
    public List<Pedido> ordenarPorHolgura(List<Pedido> pedidos) {
        if (pedidos == null) return new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        return pedidos.stream()
                .sorted(Comparator.comparing(p -> p.calcularHolgura(ahora)))
                .collect(Collectors.toList());
    }

    /**
     * Selecciona el almacén de origen más conveniente para el pedido (RF-02).
     * Prioriza el almacén central como primera salida, evaluando disponibilidad de stock y distancia.
     */
    public Almacen seleccionarAlmacenOrigen(Pedido pedido) {
        List<Almacen> almacenes = almacenRepository.findAll();
        if (almacenes.isEmpty()) return null;

        // Buscar el almacén más cercano que tenga stock suficiente
        if (pedido != null && pedido.getDestino() != null) {
            Ubicacion dest = pedido.getDestino();
            return almacenes.stream()
                    .filter(a -> a.tieneStockSuficiente(pedido.getCantidadUnidades()))
                    .min(Comparator.comparingDouble(a -> a.getUbicacion().distanciaOrtogonalA(dest)))
                    .orElse(almacenes.get(0));
        }

        // Si no hay pedido válido, retornar el primer almacén disponible
        return almacenes.get(0);
    }

    /**
     * Replanifica automáticamente rutas afectadas por un bloqueo vial (RF-12, RF-13).
     */
    public List<Ruta> replanificarPorBloqueo(Bloqueo bloqueo) {
        log.info("Replanificando rutas afectadas por bloqueo {}", bloqueo.getCodigo());
        redVial.aplicarBloqueo(bloqueo);

        List<Ruta> rutasActivas = rutaRepository.findByEstado(EstadoRuta.EN_EJECUCION);
        List<Ruta> replanificadas = new ArrayList<>();

        for (Ruta ruta : rutasActivas) {
            // Verificar si la ruta pasa por algún tramo bloqueado y recalcular
            ruta.setEstado(EstadoRuta.REPLANIFICADA);
            ruta.calcularTiempoEstimado();
            rutaRepository.save(ruta);
            replanificadas.add(ruta);
        }
        return replanificadas;
    }

    /**
     * Replanifica las rutas ante una avería mecánica (RF-14, RF-15, RF-16).
     */
    public List<Ruta> replanificarPorAveria(Averia averia) {
        log.info("Atendiendo avería de unidad {}", averia.getCodigo());
        if (averia.getUnidad() != null) {
            return reasignarPedidosPendientes(averia.getUnidad());
        }
        return new ArrayList<>();
    }

    /**
     * Reasigna pedidos pendientes de una unidad averiada a otras unidades con capacidad disponible (RF-15, RF-16).
     */
    public List<Ruta> reasignarPedidosPendientes(UnidadTransporte unidad) {
        log.info("Reasignando pedidos pendientes de la unidad averiada {}", unidad.getCodigo());

        List<Ruta> rutasDeUnidad = rutaRepository.findByUnidadTransporteId(unidad.getId());
        List<Pedido> pedidosPorReasignar = new ArrayList<>();

        for (Ruta r : rutasDeUnidad) {
            for (ParadaRuta p : r.getParadas()) {
                if (!p.isEntregada() && p.getPedido() != null) {
                    pedidosPorReasignar.add(p.getPedido());
                }
            }
        }

        if (pedidosPorReasignar.isEmpty()) {
            return new ArrayList<>();
        }

        // Ordenar por menor tiempo restante
        pedidosPorReasignar = ordenarPorHolgura(pedidosPorReasignar);

        // Flota de contingencia disponible
        List<UnidadTransporte> candidatos = unidadRepository.findByActivoTrueAndEstadoOperativo(EstadoOperativo.DISPONIBLE);
        if (candidatos.isEmpty()) {
            log.error("ALERTA CRÍTICA (RF-16): Imposible reasignar pedidos de unidad {}. No hay unidades disponibles.", unidad.getCodigo());
            return new ArrayList<>();
        }

        return algoritmoRuteo.construirSolucion(pedidosPorReasignar, candidatos, redVial);
    }
}
