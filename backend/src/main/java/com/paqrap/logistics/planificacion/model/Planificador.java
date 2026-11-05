package com.paqrap.logistics.planificacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.flota.service.PlanMantenimientoService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    /** Opcional (por setter) para no romper a quienes construyen el Planificador a mano. */
    private PlanMantenimientoService planMantenimiento;

    @Autowired(required = false)
    public void setPlanMantenimiento(PlanMantenimientoService planMantenimiento) {
        this.planMantenimiento = planMantenimiento;
    }

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

        // Ordenar pedidos de menor a mayor holgura (RF-04) según el instante simulado
        List<Pedido> pedidosOrdenados = ordenarPorHolgura(pedidosPendientes, instante);

        // Obtener flota disponible
        List<UnidadTransporte> flotaDisponible = unidadRepository.findByDadaDeBajaFalseAndEstadoOperativo(EstadoOperativo.DISPONIBLE);
        if (flotaDisponible.isEmpty()) {
            log.warn("No hay unidades de transporte disponibles para planificar rutas.");
            return new ArrayList<>();
        }

        // Construir solución usando el algoritmo configurado, sin unidades en mantenimiento (FAQ 19)
        List<Ruta> rutasGeneradas = construirSolucionRespetandoMantenimiento(pedidosOrdenados, flotaDisponible, instante);

        List<Ruta> rutasConfirmadas = new ArrayList<>();
        for (Ruta ruta : rutasGeneradas) {
            // Asignar almacén central por defecto o el más cercano
            Almacen origen = almacenDeSalida(ruta,
                    seleccionarAlmacenOrigen(ruta.getParadas().isEmpty() ? null : ruta.getParadas().get(0).getPedido()),
                    almacenRepository.findAll());
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
                            p.getPedido().setUnidadAsignadaId(ruta.getUnidadTransporte().getCodigo());
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
     * Ejecuta el algoritmo de ruteo respetando el plan de mantenimiento preventivo (FAQ 19):
     * <ol>
     *   <li>No se planifican unidades que están en su día de mantenimiento.</li>
     *   <li>Se rechaza toda ruta que dejaría a su unidad en ruta cuando empieza su mantenimiento
     *       (a las 00:00); sus pedidos se reasignan una vez entre las unidades que quedaron libres.
     *       Los que no se puedan asignar quedan pendientes para el siguiente ciclo.</li>
     * </ol>
     * Sin plan de mantenimiento configurado se comporta igual que el algoritmo directo.
     */
    public List<Ruta> construirSolucionRespetandoMantenimiento(List<Pedido> pedidosOrdenados,
                                                               List<UnidadTransporte> flota,
                                                               LocalDateTime instante) {
        if (planMantenimiento == null || instante == null) {
            List<Ruta> directas = algoritmoRuteo.construirSolucion(pedidosOrdenados, flota, redVial);
            alinearConInstante(directas, inicioDeSolucion(directas), instante);
            return directas;
        }
        List<UnidadTransporte> candidatos = flota.stream()
                .filter(u -> !planMantenimiento.estaEnMantenimiento(u.getCodigo(), instante))
                .collect(Collectors.toList());
        List<Ruta> rutas = algoritmoRuteo.construirSolucion(pedidosOrdenados, candidatos, redVial);

        List<Ruta> validas = new ArrayList<>();
        List<Pedido> sinAsignar = new ArrayList<>();
        Set<String> usadas = new HashSet<>();
        Set<String> excluidas = new HashSet<>();
        LocalDateTime base = inicioDeSolucion(rutas);
        for (Ruta r : rutas) {
            String codigo = r.getUnidadTransporte() != null ? r.getUnidadTransporte().getCodigo() : null;
            if (cruzaMantenimiento(r, base, instante)) {
                excluidas.add(codigo);
                r.getParadas().stream().map(ParadaRuta::getPedido).filter(Objects::nonNull).forEach(sinAsignar::add);
            } else {
                validas.add(r);
                usadas.add(codigo);
            }
        }
        // Las rutas de la primera solución salen en el instante de planificación
        alinearConInstante(validas, base, instante);
        if (sinAsignar.isEmpty()) {
            return validas;
        }
        log.info("{} rutas rechazadas porque su unidad entra a mantenimiento antes de terminarlas ({}). Reasignando {} pedidos.",
                excluidas.size(), excluidas, sinAsignar.size());

        List<UnidadTransporte> restantes = candidatos.stream()
                .filter(u -> !usadas.contains(u.getCodigo()) && !excluidas.contains(u.getCodigo()))
                .collect(Collectors.toList());
        if (!restantes.isEmpty()) {
            List<Ruta> reintento = algoritmoRuteo.construirSolucion(ordenarPorHolgura(sinAsignar, instante), restantes, redVial);
            LocalDateTime baseReintento = inicioDeSolucion(reintento);
            List<Ruta> reintentoValidas = reintento.stream()
                    .filter(r -> !cruzaMantenimiento(r, baseReintento, instante))
                    .collect(Collectors.toList());
            alinearConInstante(reintentoValidas, baseReintento, instante);
            validas.addAll(reintentoValidas);
        }
        return validas;
    }

    /**
     * El algoritmo fecha sus rutas desde el pedido pendiente más antiguo, no desde ahora: una ruta
     * planificada a las 09:00 salía "a las 08:08", y el motor daba por entregadas al instante las
     * paradas cuya hora estimada ya había pasado (los vehículos aparecían de golpe a mitad de camino).
     * Un vehículo no puede salir en el pasado: se corre toda la solución, con sus rutas encadenadas,
     * para que la primera salga en {@code instante}. Los tiempos entre paradas no cambian.
     *
     * @param base instante en que el algoritmo hizo salir a la primera ruta
     */
    static void alinearConInstante(List<Ruta> rutas, LocalDateTime base, LocalDateTime instante) {
        if (rutas == null || base == null || instante == null || !base.isBefore(instante)) return;
        Duration corrimiento = Duration.between(base, instante);
        for (Ruta r : rutas) {
            if (r.getFechaHoraGeneracion() != null) {
                r.setFechaHoraGeneracion(r.getFechaHoraGeneracion().plus(corrimiento));
            }
            for (ParadaRuta p : r.getParadas()) {
                if (p.getHoraEstimadaLlegada() != null) {
                    p.setHoraEstimadaLlegada(p.getHoraEstimadaLlegada().plus(corrimiento));
                }
            }
        }
    }

    /**
     * true si la unidad de la ruta empieza un mantenimiento antes de terminar la ruta.
     * El algoritmo fecha sus rutas con su propio reloj, así que la hora de fin se calcula relativa
     * al inicio de la solución ({@code base}) y se traslada al instante de planificación.
     */
    boolean cruzaMantenimiento(Ruta ruta, LocalDateTime base, LocalDateTime instante) {
        if (planMantenimiento == null || ruta.getUnidadTransporte() == null || base == null) return false;
        LocalDateTime finRuta = ruta.getParadas().stream()
                .filter(p -> p.getHoraEstimadaLlegada() != null)
                .map(p -> p.getHoraEstimadaLlegada().plusMinutes(p.getTiempoServicioMin()))
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (finRuta == null) return false;
        LocalDateTime finEstimado = instante.plus(Duration.between(base, finRuta));
        return planMantenimiento
                .mantenimientoQueIniciaEntre(ruta.getUnidadTransporte().getCodigo(), instante, finEstimado)
                .isPresent();
    }

    private static LocalDateTime inicioDeSolucion(List<Ruta> rutas) {
        return rutas.stream()
                .map(Ruta::getFechaHoraGeneracion)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * Ordena los pedidos pendientes de menor a mayor holgura (RF-04) según el instante temporal.
     * Los pedidos con holgura por debajo del umbral crítico son priorizados primero.
     */
    public List<Pedido> ordenarPorHolgura(List<Pedido> pedidos, LocalDateTime instante) {
        if (pedidos == null) return new ArrayList<>();
        LocalDateTime ahora = (instante != null) ? instante : LocalDateTime.now();

        return pedidos.stream()
                .sorted(Comparator.comparing(p -> p.calcularHolgura(ahora)))
                .collect(Collectors.toList());
    }

    /**
     * Sobrecarga para mantener compatibilidad con invocaciones que no especifican instante.
     */
    public List<Pedido> ordenarPorHolgura(List<Pedido> pedidos) {
        return ordenarPorHolgura(pedidos, LocalDateTime.now());
    }

    /**
     * Almacén del que sale una ruta: si el vehículo ya está en un almacén con stock suficiente para toda
     * la ruta, sale de ahí (FAQ 10: puede recargar en cualquier almacén con stock); si no, el sugerido.
     * Evita que un vehículo que volvió a un intermedio "salte" al central al empezar su siguiente ruta.
     */
    public static Almacen almacenDeSalida(Ruta ruta, Almacen sugerido, java.util.Collection<? extends Almacen> almacenes) {
        UnidadTransporte u = ruta.getUnidadTransporte();
        if (u == null || u.getUbicacionActual() == null || almacenes == null) return sugerido;
        int unidades = ruta.getParadas().stream()
                .mapToInt(p -> p.getPedido() != null ? p.getPedido().getCantidadUnidades() : 0)
                .sum();
        for (Almacen a : almacenes) {
            if (a.getUbicacion() != null
                    && a.getUbicacion().getPosX() == u.getUbicacionActual().getPosX()
                    && a.getUbicacion().getPosY() == u.getUbicacionActual().getPosY()
                    && a.tieneStockSuficiente(unidades)) {
                return a;
            }
        }
        return sugerido;
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

        List<Ruta> rutasDeUnidad = rutaRepository.findByUnidadTransporteCodigo(unidad.getCodigo());
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
        List<UnidadTransporte> candidatos = unidadRepository.findByDadaDeBajaFalseAndEstadoOperativo(EstadoOperativo.DISPONIBLE);
        if (candidatos.isEmpty()) {
            log.error("ALERTA CRÍTICA (RF-16): Imposible reasignar pedidos de unidad {}. No hay unidades disponibles.", unidad.getCodigo());
            return new ArrayList<>();
        }

        return algoritmoRuteo.construirSolucion(pedidosPorReasignar, candidatos, redVial);
    }
}
