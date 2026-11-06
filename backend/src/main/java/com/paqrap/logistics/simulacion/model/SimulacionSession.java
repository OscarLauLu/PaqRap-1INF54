package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.dto.UnidadTransporteDTO;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.Turno;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.flota.service.PlanMantenimientoService;
import com.paqrap.logistics.pedidos.model.Cliente;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.dto.ParadaRutaDTO;
import com.paqrap.logistics.planificacion.dto.RutaDTO;
import com.paqrap.logistics.planificacion.model.CaminoRuta;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Planificador;
import com.paqrap.logistics.planificacion.model.PlanificadorTrasvase;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.redvial.service.RedVialService;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Representa una sesión aislada de simulación para un dispositivo o usuario específico.
 * Gobierna su propio reloj, hilo de ejecución programado, unidades vehiculares,
 * órdenes activas, rutas calculadas, métricas acumuladas y persistencia de resultados (RF-64 a RF-75).
 */
@Slf4j
@Getter
@Setter
public class SimulacionSession {

    private static final AtomicLong RUTA_ID_SEQ = new AtomicLong(1000);
    private static final AtomicLong PEDIDO_ID_SEQ = new AtomicLong(500000);

    private final String sessionId;
    private final RelojSimulado reloj;
    private volatile EstadoEjecucion estado = EstadoEjecucion.CONFIGURADA;
    private volatile TipoEscenario escenarioActual = TipoEscenario.DIA_A_DIA;
    private ParametrosSimulacion parametros;
    private LocalDateTime ultimoAcceso = LocalDateTime.now();

    private ScheduledExecutorService executorService;
    private LocalDateTime instanteInicioEjecucionReal;
    private LocalDateTime ultimaHoraPlanificada;
    @Getter
    private double factorBase = 1.0;

    // Estado en memoria de la sesión
    private final List<UnidadTransporte> unidades = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosProgramados = new CopyOnWriteArrayList<>();
    private final List<Pedido> pedidosActivos = new CopyOnWriteArrayList<>();
    private final List<Bloqueo> bloqueosProgramados = new CopyOnWriteArrayList<>();
    private final List<Bloqueo> bloqueosActivos = new CopyOnWriteArrayList<>();
    private final List<Averia> averiasProgramadas = new CopyOnWriteArrayList<>();
    private final List<Averia> averiasActivas = new CopyOnWriteArrayList<>();
    private final List<Ruta> rutas = new CopyOnWriteArrayList<>();

    // Métricas acumuladas en tiempo real de esta sesión
    private int totalPedidosIngresados = 0;
    private int pedidosEnPlazo = 0;
    private int pedidosTarde = 0;
    private double costoAuto = 0.0;
    private double costoMoto = 0.0;
    private double costoBici = 0.0;
    private int entregasAuto = 0;
    private int entregasMoto = 0;
    private int entregasBici = 0;
    private int bloqueosOcurridos = 0;
    private int averiasOcurridas = 0;
    private LocalDateTime instanteColapsoDetectado;
    private Integer volumenPedidosColapsoDetectado;
    /** Código del pedido cuyo incumplimiento provocó el colapso (para el aviso de emergencia). */
    private String codigoPedidoColapso;

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    // Carga de datos por mes y mantenimiento preventivo
    private CargadorArchivos cargador;
    private RedVial redVial;
    private PlanMantenimientoService planMantenimiento;
    private AlmacenRepository almacenRepo;
    /** true = pedidos y bloqueos salen de los archivos oficiales del mes simulado. */
    private boolean datosPorFecha;
    private YearMonth ultimoMesCargado;
    /** Unidades que esta sesión pasó a EN_MANTENIMIENTO por el plan (para devolverlas a DISPONIBLE). */
    private final Set<String> enMantenimientoPorPlan = ConcurrentHashMap.newKeySet();
    /** Averías que ya tienen una ruta de rescate despachada */
    private final Set<String> averiasConRescate = ConcurrentHashMap.newKeySet();

    public SimulacionSession(String sessionId) {
        this.sessionId = (sessionId != null && !sessionId.trim().isEmpty()) ? sessionId.trim() : UUID.randomUUID().toString();
        this.reloj = new RelojSimulado();
    }

    public void actualizarAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }

    public double getCostoAcumuladoTotal() {
        return costoAuto + costoMoto + costoBici;
    }

    public void reiniciarMetricas() {
        this.totalPedidosIngresados = 0;
        this.pedidosEnPlazo = 0;
        this.pedidosTarde = 0;
        this.costoAuto = 0.0;
        this.costoMoto = 0.0;
        this.costoBici = 0.0;
        this.entregasAuto = 0;
        this.entregasMoto = 0;
        this.entregasBici = 0;
        this.bloqueosOcurridos = 0;
        this.averiasOcurridas = 0;
        this.instanteColapsoDetectado = null;
        this.volumenPedidosColapsoDetectado = null;
        this.codigoPedidoColapso = null;
        this.ultimaHoraPlanificada = null;
    }

    public void inicializarFlota(UnidadTransporteRepository unidadRepo) {
        unidades.clear();
        enMantenimientoPorPlan.clear();
        List<UnidadTransporte> base = unidadRepo.findAll();
        for (UnidadTransporte u : base) {
            UnidadTransporte copia = UnidadTransporte.builder()
                    .codigo(u.getCodigo())
                    .tipo(u.getTipo())
                    .estadoOperativo(EstadoOperativo.DISPONIBLE)
                    .ubicacionActual(new Ubicacion(27, 14))
                    .cargaActual(0)
                    .dadaDeBaja(u.isDadaDeBaja())
                    .build();
            unidades.add(copia);
        }
        log.info("Sesión {}: Flota inicializada con {} vehículos independientes.", sessionId, unidades.size());
    }

    public void configurar(ParametrosSimulacion params, CargadorArchivos cargador) {
        if (params == null || !params.validar()) {
            throw new IllegalArgumentException("Parámetros de simulación inválidos.");
        }
        this.parametros = params;
        this.estado = EstadoEjecucion.CONFIGURADA;

        pedidosProgramados.clear();
        bloqueosProgramados.clear();

        if (params.getArchivoPedidos() != null && cargador != null) {
            List<Pedido> cargados = cargador.cargarPedidos(params.getArchivoPedidos());
            pedidosProgramados.addAll(cargados);
        }
        if (params.getArchivoBloqueos() != null && cargador != null) {
            List<Bloqueo> cargados = cargador.cargarBloqueos(params.getArchivoBloqueos());
            bloqueosProgramados.addAll(cargados);
        }

        if (params.getFactorAceleracion() != null && params.getFactorAceleracion() > 0) {
            reloj.setFactorAceleracion(params.getFactorAceleracion());
        }
        if (params.getFechaHoraInicio() != null) {
            reloj.inicializar(params.getFechaHoraInicio(), reloj.getFactorAceleracion());
        }

        log.info("Sesión {} configurada: {} pedidos y {} bloqueos programados.",
                sessionId, pedidosProgramados.size(), bloqueosProgramados.size());
    }

    public ResultadoSimulacion ejecutar(
            TipoEscenario escenario,
            Planificador planificador,
            RedVial redVial,
            AlmacenRepository almacenRepo,
            CargadorArchivos cargador,
            UnidadTransporteRepository unidadRepo,
            ResultadoSimulacionRepository resultadoRepo,
            PlanMantenimientoService planMantenimiento) {

        actualizarAcceso();
        this.redVial = redVial;
        this.cargador = cargador;
        this.almacenRepo = almacenRepo;
        this.planMantenimiento = planMantenimiento;

        LocalDateTime fechaElegida = (parametros != null) ? parametros.getFechaHoraInicio() : null;
        LocalDateTime inicioSim = (fechaElegida != null)
                ? fechaElegida : LocalDate.now(ZONA_LIMA).atStartOfDay();

        // Sin archivo explícito, los pedidos y bloqueos salen de los archivos oficiales del mes de
        // inicio; los meses siguientes se cargan cuando el reloj llega a ellos (ver procesarEventos).
        datosPorFecha = (parametros == null || parametros.getArchivoPedidos() == null);
        if (datosPorFecha) {
            pedidosProgramados.clear();
            bloqueosProgramados.clear();
            ultimoMesCargado = null;
            cargarDatosHasta(YearMonth.from(inicioSim));
        } else {
            // Se relee el archivo en cada corrida: la anterior consumió los pedidos programados
            configurar(parametros, cargador);
            aplicarEnRedVial(bloqueosProgramados);
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        this.estado = EstadoEjecucion.EN_EJECUCION;
        this.instanteInicioEjecucionReal = LocalDateTime.now();
        this.escenarioActual = escenario;
        reiniciarMetricas();

        pedidosActivos.clear();
        bloqueosActivos.clear();
        averiasActivas.clear();
        averiasConRescate.clear();
        rutas.clear();

        inicializarFlota(unidadRepo);

        double factorAceleracion;
        if (parametros != null && parametros.getFactorAceleracion() != null && parametros.getFactorAceleracion() > 0) {
            factorAceleracion = parametros.getFactorAceleracion();
        } else {
            switch (escenario) {
                case SIMULACION_5D:
                    factorAceleracion = 150.0;
                    break;
                case COLAPSO_LOGISTICO:
                    factorAceleracion = 50.0;
                    break;
                case DIA_A_DIA:
                default:
                    factorAceleracion = 1.0;
            }
        }
        this.factorBase = factorAceleracion;
        reloj.inicializar(inicioSim, factorAceleracion);

        // Filtrar pedidos anteriores a la fecha elegida
        final LocalDateTime desde = inicioSim;
        int totalAntes = pedidosProgramados.size();
        pedidosProgramados.removeIf(p -> p.getFechaHoraRegistro() != null && p.getFechaHoraRegistro().isBefore(desde));
        log.info("Sesión {}: Pedidos considerados desde {}: {} (se omitieron {} anteriores)",
                sessionId, desde, pedidosProgramados.size(), totalAntes - pedidosProgramados.size());

        // Arrancar ciclo periódico de simulación en hilo propio
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                avanzarPasoSimulado(Duration.ofMillis(500), planificador, redVial, almacenRepo, resultadoRepo);
            } catch (Exception e) {
                log.error("Sesión {}: Error en paso de simulación: {}", sessionId, e.getMessage(), e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        ResultadoSimulacion inicial = new ResultadoSimulacion();
        inicial.setCodigo("SIM-" + UUID.randomUUID().toString().substring(0, 8));
        inicial.setSessionId(sessionId);
        inicial.setEscenario(escenario.name());
        inicial.setFechaHoraInicio(inicioSim);
        inicial.setTotalPedidos(pedidosProgramados.size());
        return inicial;
    }

    public synchronized void avanzarPasoSimulado(
            Duration avanceTiempoReal,
            Planificador planificador,
            RedVial redVial,
            AlmacenRepository almacenRepo,
            ResultadoSimulacionRepository resultadoRepo) {

        if (estado != EstadoEjecucion.EN_EJECUCION) return;

        reloj.avanzar(avanceTiempoReal);
        LocalDateTime instanteActual = reloj.getInstanteActual();

        // 1. Procesar eventos (nuevos pedidos, bloqueos) y mantenimiento preventivo del día (FAQ 19)
        procesarEventos(instanteActual);
        aplicarMantenimiento(instanteActual);
        List<Almacen> almacenes = almacenRepo.findAll();
        atenderAverias(instanteActual, almacenes);

        if (estado != EstadoEjecucion.EN_EJECUCION) return;

        // 2. Movimientos de flota y entregas
        ejecutarMovimientosFlota(instanteActual, almacenes);

        if (estado != EstadoEjecucion.EN_EJECUCION) return;

        // 3. Planificación periódica
        LocalDateTime bloqueActual = (escenarioActual == TipoEscenario.DIA_A_DIA)
                ? instanteActual.truncatedTo(ChronoUnit.MINUTES).withMinute(instanteActual.getMinute() / 5 * 5)
                : instanteActual.truncatedTo(ChronoUnit.HOURS);

        if (ultimaHoraPlanificada == null || bloqueActual.isAfter(ultimaHoraPlanificada)) {
            ultimaHoraPlanificada = bloqueActual;
            ejecutarPlanificacion(instanteActual, planificador, redVial, almacenRepo);
        }

        // Carga que lleva cada vehículo tras las entregas y las rutas nuevas de este paso
        actualizarCargas();

        // 4. Recarga diaria a las 23:59 (RF-20)
        LocalTime hora = reloj.horaSimulada();
        if (hora.getHour() == 23 && hora.getMinute() >= 50) {
            ejecutarRecargaDiaria(almacenRepo);
        }

        // 5. Finalizar automáticamente SIMULACION_5D al alcanzar 5 días (120 h)
        LocalDateTime inicioSim = reloj.getInstanteInicio();
        if (escenarioActual == TipoEscenario.SIMULACION_5D && inicioSim != null) {
            if (!instanteActual.isBefore(inicioSim.plusDays(5))) {
                log.info("Sesión {}: Simulación 5D completada exitosamente a los 5 días ({}).", sessionId, instanteActual);
                detener(resultadoRepo);
                this.estado = EstadoEjecucion.FINALIZADA;
                return;
            }
        }

        // 6. Verificar condición de colapso logístico (RF-73)
        if (escenarioActual == TipoEscenario.COLAPSO_LOGISTICO && detectarColapso()) {
            this.instanteColapsoDetectado = instanteActual;
            this.volumenPedidosColapsoDetectado = contarPedidosActivos();
            log.warn("Sesión {}: ¡COLAPSO LOGÍSTICO DETECTADO en {}! Pedidos activos: {}",
                    sessionId, instanteActual, volumenPedidosColapsoDetectado);
            detener(resultadoRepo);
            this.estado = EstadoEjecucion.DETENIDA_POR_COLAPSO;
        }
    }

    /**
     * Carga los archivos oficiales de ventas y bloqueos de cada mes pendiente hasta {@code objetivo}
     * (inclusive). Un 5D que cruza de mes o un colapso largo siguen recibiendo pedidos.
     */
    void cargarDatosHasta(YearMonth objetivo) {
        if (cargador == null || objetivo == null) return;
        YearMonth mes = (ultimoMesCargado == null) ? objetivo : ultimoMesCargado.plusMonths(1);
        while (!mes.isAfter(objetivo)) {
            final YearMonth mesActual = mes;
            DirectorioDatos.archivoVentas(mesActual).ifPresentOrElse(
                    ruta -> pedidosProgramados.addAll(cargador.cargarPedidos(ruta)),
                    () -> log.warn("Sesión {}: no hay archivo de ventas para {}", sessionId, mesActual));
            DirectorioDatos.archivoBloqueos(mesActual).ifPresent(ruta -> {
                List<Bloqueo> delMes = cargador.cargarBloqueos(ruta);
                bloqueosProgramados.addAll(delMes);
                aplicarEnRedVial(delMes);
            });
            ultimoMesCargado = mesActual;
            mes = mes.plusMonths(1);
        }
    }

    /**
     * Los bloqueos se aplican a la red vial para que el ACO y el camino dibujado los esquiven (FAQ 7).
     * La red guarda la ventana de cada bloqueo y filtra por la hora de cada consulta, así que es
     * compartida entre sesiones sin que una afecte a otra en otra fecha.
     */
    private void aplicarEnRedVial(List<Bloqueo> bloqueos) {
        if (redVial == null || bloqueos == null) return;
        bloqueos.forEach(redVial::aplicarBloqueo);
    }

    /** Bloqueos vigentes en el reloj de esta sesión, para dibujarlos en el mapa. */
    public List<Bloqueo> obtenerBloqueosVigentes() {
        return new ArrayList<>(bloqueosActivos);
    }

    /**
     * Mantenimiento preventivo (FAQ 19): la unidad no está disponible de 00:00 a 23:59 de su día.
     * Solo se pasa a mantenimiento una unidad libre; la planificación ya evita que una unidad siga
     * en ruta a las 00:00 de ese día (Planificador#construirSolucionRespetandoMantenimiento).
     */
    private void aplicarMantenimiento(LocalDateTime instante) {
        if (planMantenimiento == null || instante == null) return;
        for (UnidadTransporte u : unidades) {
            boolean enPlan = planMantenimiento.estaEnMantenimiento(u.getCodigo(), instante);
            if (enPlan && u.getEstadoOperativo() == EstadoOperativo.DISPONIBLE) {
                u.setEstadoOperativo(EstadoOperativo.EN_MANTENIMIENTO);
                enMantenimientoPorPlan.add(u.getCodigo());
            } else if (!enPlan && enMantenimientoPorPlan.remove(u.getCodigo())
                    && u.getEstadoOperativo() == EstadoOperativo.EN_MANTENIMIENTO) {
                u.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
            }
        }
    }

    /**
     * Carga actual de cada vehículo = paquetes aún no entregados de su ruta en curso.
     * Antes nadie actualizaba este campo y el mapa mostraba "0/8 paquetes" aunque el vehículo
     * estuviera repartiendo.
     */
    public void actualizarCargas() {
        LocalDateTime instante = reloj.getInstanteActual();
        Map<String, Ruta> enCurso = Ruta.rutaEnCursoPorUnidad(rutas);
        // Carga de rescates aún no trasvasada: sigue a bordo de la unidad averiada
        Map<String, Integer> porTrasvasar = new HashMap<>();
        for (Ruta r : rutas) {
            if (r.getEstado() != EstadoRuta.COMPLETADA && r.trasvasePendiente(instante)) {
                porTrasvasar.merge(r.getRescate().getUnidadAveriada(), r.cargaPendiente(), Integer::sum);
            }
        }
        for (UnidadTransporte u : unidades) {
            Ruta r = enCurso.get(u.getCodigo());
            int carga = (r != null ? r.cargaABordo(instante) : 0) + porTrasvasar.getOrDefault(u.getCodigo(), 0);
            u.setCargaActual(carga);
        }
    }

    /**
     * Registra una avería en esta sesión (FAQ 3: las averías se registran en el visualizador). La unidad
     * queda inmóvil en {@code ubicacion} (la posición en la que el operario la ve en el mapa).
     */
    public synchronized Averia registrarAveria(String codigoUnidad, TipoAveria tipo, Ubicacion ubicacion) {
        UnidadTransporte u = unidades.stream()
                .filter(x -> x.getCodigo().equals(codigoUnidad))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("UnidadTransporte", "codigo", codigoUnidad));
        if (u.getEstadoOperativo() == EstadoOperativo.AVERIADA) {
            throw new BusinessException("La unidad " + codigoUnidad + " ya está averiada.");
        }
        Averia a = Averia.builder()
                .codigo("AVE-" + codigoUnidad + "-" + (averiasOcurridas + 1))
                .tipo(tipo)
                .fechaHoraEvento(reloj.getInstanteActual())
                .ubicacionFalla(ubicacion != null ? ubicacion : u.getUbicacionActual())
                .origenManual(true)
                .build();
        AtencionAverias.iniciar(a, u);
        averiasActivas.add(a);
        averiasOcurridas++;
        log.info("Sesión {}: avería {} de {} en {} (reincorpora {})", sessionId, tipo, codigoUnidad,
                a.getUbicacionFalla(), a.getHoraReincorporacion());
        return a;
    }

    /** Avanza las averías activas: traslado al central (tipo 2 y 3) y reincorporación. */
    private void atenderAverias(LocalDateTime instante, List<Almacen> almacenes) {
        if (averiasActivas.isEmpty()) return;
        Ubicacion central = almacenes.stream()
                .filter(a -> a instanceof AlmacenCentral && a.getUbicacion() != null)
                .map(Almacen::getUbicacion)
                .findFirst()
                .orElse(new Ubicacion(27, 14));
        Map<String, Ruta> enCurso = Ruta.rutaEnCursoPorUnidad(rutas);
        for (Averia a : averiasActivas) {
            UnidadTransporte u = a.getUnidad();
            if (u == null) continue;
            // Rescate: una unidad libre o que regresa va a trasvasar la carga (una vez por avería)
            if (!averiasConRescate.contains(a.getCodigo())) {
                Ruta rescate = PlanificadorTrasvase.planificar(a, enCurso.get(u.getCodigo()), unidades, enCurso,
                        instante, almacenes, "RES-" + a.getCodigo());
                if (rescate != null) {
                    rescate.setId(RUTA_ID_SEQ.getAndIncrement());
                    rescate.getParadas().forEach(p -> {
                        if (p.getPedido() != null) p.getPedido().setRutaAsignadaId(rescate.getId());
                    });
                    rutas.add(rescate);
                    registrarCostosYRutas(List.of(rescate));
                    averiasConRescate.add(a.getCodigo());
                    enCurso = Ruta.rutaEnCursoPorUnidad(rutas);
                    log.info("Sesión {}: {} va a rescatar {} paquetes de {} (trasvase {} a {})", sessionId,
                            rescate.getUnidadTransporte().getCodigo(), rescate.getRescate().getCantidadProductos(),
                            u.getCodigo(), rescate.getRescate().getLlegada(), rescate.getRescate().getFin());
                }
            }
            AtencionAverias.avanzar(a, u, enCurso.get(u.getCodigo()), instante, central, null);
        }
        averiasActivas.removeIf(Averia::isResuelta);
    }

    /** "TIPO_1", "TIPO_2" o "TIPO_3" si la unidad tiene una avería activa; null si no. */
    private String tipoAveriaActiva(String codigoUnidad) {
        for (Averia a : averiasActivas) {
            if (!a.isResuelta() && a.getUnidad() != null && codigoUnidad.equals(a.getUnidad().getCodigo())) {
                return a.getTipo() != null ? a.getTipo().name() : null;
            }
        }
        return null;
    }

    private void procesarEventos(LocalDateTime instante) {
        if (instante == null) return;

        if (datosPorFecha) {
            cargarDatosHasta(YearMonth.from(instante));
        }

        // Ingreso de nuevos pedidos
        List<Pedido> entrantes = new ArrayList<>();
        for (Pedido p : pedidosProgramados) {
            if (p.getFechaHoraRegistro() != null && !p.getFechaHoraRegistro().isAfter(instante)) {
                entrantes.add(p);
            }
        }
        for (Pedido p : entrantes) {
            if (p.getId() == null) {
                p.setId(PEDIDO_ID_SEQ.getAndIncrement());
            }
            if (p.getCliente() == null) {
                p.setCliente(Cliente.builder()
                        .idCliente("CLI-" + p.getCodigo())
                        .nombre("Cliente " + p.getCodigo())
                        .ubicacionEntrega(p.getDestino())
                        .build());
            }
            p.setEstado(EstadoPedido.REGISTRADO);
            pedidosActivos.add(p);
            totalPedidosIngresados++;
        }
        pedidosProgramados.removeAll(entrantes);

        // Bloqueos
        for (Bloqueo b : bloqueosProgramados) {
            if (b.estaVigente(instante) && !b.isActivo()) {
                b.activar();
                if (!bloqueosActivos.contains(b)) {
                    bloqueosActivos.add(b);
                }
                bloqueosOcurridos++;
            } else if (!b.estaVigente(instante) && b.isActivo()) {
                b.desactivar();
                bloqueosActivos.remove(b);
            }
        }
    }

    private void ejecutarMovimientosFlota(LocalDateTime instante, List<Almacen> almacenes) {
        List<Ruta> copiaRutas = new ArrayList<>(this.rutas);
        for (Ruta r : copiaRutas) {
            if (r.getEstado() == EstadoRuta.COMPLETADA || r.getUnidadTransporte() == null) continue;
            // Una unidad averiada se queda inmóvil donde ocurrió la falla (FAQ 3)
            if (r.getUnidadTransporte().getEstadoOperativo() == EstadoOperativo.AVERIADA) continue;

            if (Turno.esHoraAlimentacion(instante.toLocalTime())) {
                continue;
            }

            // Ruta de rescate: primero va al punto de la avería y trasvasa (30 min)
            if (r.avanzarTramoRescate(instante)) continue;

            if (r.getEstado() == EstadoRuta.PLANIFICADA) {
                r.setEstado(EstadoRuta.EN_EJECUCION);
            }

            boolean todasEntregadas = true;
            for (ParadaRuta p : r.getParadas()) {
                if (!p.isEntregada()) {
                    todasEntregadas = false;

                    if (p.getHoraEstimadaLlegada() != null && !instante.isBefore(p.getHoraEstimadaLlegada())) {
                        p.registrarEntrega(instante);
                        if (p.getPedido() != null && p.getPedido().getDestino() != null) {
                            r.getUnidadTransporte().setUbicacionActual(
                                    new Ubicacion(p.getPedido().getDestino().getPosX(), p.getPedido().getDestino().getPosY()));
                            p.getPedido().setEstado(EstadoPedido.ENTREGADO);
                        }

                        if (p.getPedido() != null) {
                            if (p.getPedido().getPlazoLimiteEntrega() != null && p.getPedido().getPlazoLimiteEntrega().isBefore(instante)) {
                                pedidosTarde++;
                                if (codigoPedidoColapso == null) {
                                    codigoPedidoColapso = p.getPedido().getCodigo();
                                }
                            } else {
                                pedidosEnPlazo++;
                            }
                        }
                    } else if (p.getHoraEstimadaLlegada() != null) {
                        // Interpolación visual continua
                        int pos = r.getParadas().indexOf(p);
                        LocalDateTime startTime;
                        int startX, startY;
                        if (pos == 0) {
                            // Sale del almacén, o del punto de la avería si es una ruta de rescate
                            startTime = r.inicioEntregas();
                            Ubicacion origen = r.origenEntregas();
                            startX = origen != null ? origen.getPosX() : 27;
                            startY = origen != null ? origen.getPosY() : 14;
                        } else {
                            ParadaRuta prev = r.getParadas().get(pos - 1);
                            startTime = prev.getHoraEstimadaLlegada().plusMinutes(prev.getTiempoServicioMin());
                            startX = (prev.getPedido() != null && prev.getPedido().getDestino() != null)
                                    ? prev.getPedido().getDestino().getPosX() : 27;
                            startY = (prev.getPedido() != null && prev.getPedido().getDestino() != null)
                                    ? prev.getPedido().getDestino().getPosY() : 14;
                        }

                        if (startTime != null && p.getPedido() != null && p.getPedido().getDestino() != null) {
                            if (!instante.isBefore(startTime)) {
                                long totalMin = Duration.between(startTime, p.getHoraEstimadaLlegada()).toMinutes();
                                long elapsedMin = Duration.between(startTime, instante).toMinutes();
                                if (totalMin > 0) {
                                    double factor = (double) elapsedMin / totalMin;
                                    factor = Math.max(0.0, Math.min(1.0, factor));

                                    int endX = p.getPedido().getDestino().getPosX();
                                    int endY = p.getPedido().getDestino().getPosY();

                                    int currentX = (int) Math.round(startX + (endX - startX) * factor);
                                    int currentY = (int) Math.round(startY + (endY - startY) * factor);

                                    r.getUnidadTransporte().setUbicacionActual(new Ubicacion(currentX, currentY));
                                }
                            }
                        }
                    }
                    break;
                }
            }

            // Con todo entregado, el vehículo regresa al almacén con stock más cercano (FAQ 10):
            // se ve viajando y queda DISPONIBLE al llegar
            if (todasEntregadas && !r.getParadas().isEmpty()) {
                r.avanzarRetorno(instante, almacenes);
            }
        }
    }

    private void ejecutarPlanificacion(
            LocalDateTime instante,
            Planificador planificador,
            RedVial redVial,
            AlmacenRepository almacenRepo) {

        List<Pedido> pendientes = pedidosActivos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.REGISTRADO)
                .collect(Collectors.toList());
        if (pendientes.isEmpty()) return;

        List<Pedido> ordenados = planificador.ordenarPorHolgura(pendientes, instante);

        List<UnidadTransporte> disponibles = unidades.stream()
                .filter(u -> !u.isDadaDeBaja() && u.getEstadoOperativo() == EstadoOperativo.DISPONIBLE)
                .collect(Collectors.toList());
        if (disponibles.isEmpty()) return;

        List<Ruta> nuevasRutas = planificador.construirSolucionRespetandoMantenimiento(ordenados, disponibles, instante);

        for (Ruta ruta : nuevasRutas) {
            if (ruta.getId() == null) {
                ruta.setId(RUTA_ID_SEQ.getAndIncrement());
            }
            Almacen origen = planificador.seleccionarAlmacenOrigen(ruta.getParadas().isEmpty() ? null : ruta.getParadas().get(0).getPedido());
            ruta.setAlmacenOrigen(Planificador.almacenDeSalida(ruta, origen, almacenRepo.findAll()));

            if (!ruta.capacidadExcedida() && ruta.cumplePlazos()) {
                int parOrden = 1;
                for (ParadaRuta p : ruta.getParadas()) {
                    if (p.getId() == null) {
                        p.setId(RUTA_ID_SEQ.getAndIncrement());
                    }
                    p.setOrden(parOrden++);
                    p.setRuta(ruta);
                    if (p.getPedido() != null) {
                        p.getPedido().setEstado(EstadoPedido.PLANIFICADO);
                        p.getPedido().setRutaAsignadaId(ruta.getId());
                        if (ruta.getUnidadTransporte() != null) {
                            p.getPedido().setUnidadAsignadaId(ruta.getUnidadTransporte().getCodigo());
                        }
                    }
                }
                if (ruta.getUnidadTransporte() != null) {
                    ruta.getUnidadTransporte().cambiarEstado(EstadoOperativo.EN_RUTA);
                }
                rutas.add(ruta);
                registrarCostosYRutas(List.of(ruta));
            }
        }
    }

    private void registrarCostosYRutas(List<Ruta> nuevasRutas) {
        for (Ruta r : nuevasRutas) {
            if (r.getUnidadTransporte() != null && r.getUnidadTransporte().getTipo() != null) {
                String tipo = r.getUnidadTransporte().getTipo().getNombre().toUpperCase();
                if (tipo.contains("AUTO")) {
                    costoAuto += r.getCostoTotal();
                    entregasAuto += r.getParadas().size();
                } else if (tipo.contains("MOTO")) {
                    costoMoto += r.getCostoTotal();
                    entregasMoto += r.getParadas().size();
                } else {
                    costoBici += r.getCostoTotal();
                    entregasBici += r.getParadas().size();
                }
            }
        }
    }

    private void ejecutarRecargaDiaria(AlmacenRepository almacenRepo) {
        try {
            almacenRepo.findAll().forEach(a -> {
                if (a instanceof AlmacenIntermedio) {
                    ((AlmacenIntermedio) a).recargarDiario(reloj.getInstanteActual());
                }
            });
        } catch (Exception e) {
            log.debug("Sesión {}: Recarga diaria evaluada.", sessionId);
        }
    }

    public boolean detectarColapso() {
        if (pedidosTarde > 0) {
            return true;
        }
        LocalDateTime ahora = reloj.getInstanteActual();
        return pedidosActivos.stream()
                .filter(p -> p.getEstado() != null && !p.getEstado().esFinal())
                .filter(p -> p.getPlazoLimiteEntrega() != null && p.getPlazoLimiteEntrega().isBefore(ahora))
                .findFirst()
                .map(p -> {
                    if (codigoPedidoColapso == null) {
                        codigoPedidoColapso = p.getCodigo();
                    }
                    return true;
                })
                .orElse(false);
    }

    public int contarPedidosActivos() {
        return (int) pedidosActivos.stream()
                .filter(p -> p.getEstado() != null && !p.getEstado().esFinal())
                .count();
    }

    public void pausar() {
        if (estado == EstadoEjecucion.EN_EJECUCION) {
            estado = EstadoEjecucion.PAUSADA;
            log.info("Sesión {}: Pausada en {}", sessionId, reloj.getInstanteActual());
        }
    }

    public void reanudar() {
        if (estado == EstadoEjecucion.PAUSADA) {
            estado = EstadoEjecucion.EN_EJECUCION;
            log.info("Sesión {}: Reanudada en {}", sessionId, reloj.getInstanteActual());
        }
    }

    public void detener(ResultadoSimulacionRepository resultadoRepo) {
        EstadoEjecucion previo = estado;
        if (estado == EstadoEjecucion.EN_EJECUCION || estado == EstadoEjecucion.PAUSADA) {
            estado = EstadoEjecucion.FINALIZADA;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (previo == EstadoEjecucion.EN_EJECUCION || previo == EstadoEjecucion.PAUSADA) {
            try {
                construirResultado(resultadoRepo);
            } catch (Exception e) {
                log.error("Sesión {}: Error al guardar resultado: {}", sessionId, e.getMessage());
            }
        }
        log.info("Sesión {}: Detenida.", sessionId);
    }

    public void cambiarVelocidad(double factor) {
        double nuevoFactor = (factor <= 20.0 && factorBase > 0) ? factorBase * factor : factor;
        reloj.setFactorAceleracion(nuevoFactor);
        log.info("Sesión {}: Velocidad cambiada a {}x (factor aceleración reloj: {})", sessionId, factor, nuevoFactor);
    }

    public ResultadoSimulacion construirResultado(ResultadoSimulacionRepository resultadoRepo) {
        int entregadosTotal = pedidosEnPlazo + pedidosTarde;
        double pctCumplimiento = (entregadosTotal > 0)
                ? ((double) pedidosEnPlazo / entregadosTotal) * 100.0 : 100.0;

        ResultadoSimulacion res = ResultadoSimulacion.builder()
                .codigo("SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .sessionId(this.sessionId)
                .escenario(escenarioActual != null ? escenarioActual.name() : "DIA_A_DIA")
                .fechaHoraInicio(reloj.getInstanteInicio())
                .fechaHoraFin(reloj.getInstanteActual())
                .totalPedidos(totalPedidosIngresados)
                .pedidosEntregadosEnPlazo(pedidosEnPlazo)
                .pedidosEntregadosTarde(pedidosTarde)
                .porcentajeCumplimiento(Math.round(pctCumplimiento * 10.0) / 10.0)
                .costoTotalOperacion(costoAuto + costoMoto + costoBici)
                .costoTotalAuto(costoAuto)
                .costoTotalMoto(costoMoto)
                .costoTotalBicicleta(costoBici)
                .entregasAuto(entregasAuto)
                .entregasMoto(entregasMoto)
                .entregasBicicleta(entregasBici)
                .totalBloqueosOcurridos(bloqueosOcurridos)
                .totalAveriasOcurridas(averiasOcurridas)
                .instanteColapso(instanteColapsoDetectado)
                .volumenPedidosColapso(volumenPedidosColapsoDetectado)
                .build();
        res.guardar();
        if (resultadoRepo != null) {
            resultadoRepo.save(res);
            log.info("Sesión {}: Resultado guardado con código {}", sessionId, res.getCodigo());
        }
        return res;
    }

    // --- Mapeos para los controladores ---

    public Map<String, Object> getRelojMap() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("relojFormateado", reloj.formatoReloj());
        resp.put("diaSimulado", reloj.diaSimulado());
        resp.put("horaSimulada", reloj.horaSimulada());
        resp.put("instanteActual", reloj.getInstanteActual());
        resp.put("instanteInicio", reloj.getInstanteInicio());
        resp.put("factorAceleracion", reloj.getFactorAceleracion());
        resp.put("estadoEjecucion", estado.name());
        resp.put("escenario", escenarioActual != null ? escenarioActual.name() : null);
        return resp;
    }

    public Map<String, Object> getMetricasMap() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("totalPedidosIngresados", totalPedidosIngresados);
        resp.put("pedidosEnPlazo", pedidosEnPlazo);
        resp.put("pedidosTarde", pedidosTarde);
        resp.put("costoAcumuladoTotal", getCostoAcumuladoTotal());
        resp.put("costoAuto", costoAuto);
        resp.put("costoMoto", costoMoto);
        resp.put("costoBici", costoBici);
        resp.put("entregasAuto", entregasAuto);
        resp.put("entregasMoto", entregasMoto);
        resp.put("entregasBici", entregasBici);
        resp.put("bloqueosOcurridos", bloqueosOcurridos);
        resp.put("averiasOcurridas", averiasOcurridas);
        resp.put("instanteColapso", instanteColapsoDetectado);
        resp.put("volumenPedidosColapso", volumenPedidosColapsoDetectado);
        resp.put("codigoPedidoColapso", codigoPedidoColapso);
        return resp;
    }

    public Map<String, Object> obtenerMapaOperaciones() {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("relojSimulado", reloj.formatoReloj());
        mapa.put("instanteActual", reloj.getInstanteActual());
        mapa.put("unidades", obtenerUnidadesMap());
        mapa.put("pedidos", obtenerPedidosMap());
        mapa.put("bloqueos", bloqueosActivos);
        mapa.put("averias", averiasActivas);
        return mapa;
    }

    public List<Map<String, Object>> obtenerUnidadesMap() {
        return unidades.stream().map(u -> {
            Map<String, Object> datos = new HashMap<>();
            datos.put("id", u.getCodigo());
            datos.put("codigo", u.getCodigo());
            datos.put("tipoNombre", u.getTipo() != null ? u.getTipo().getNombre() : "DESCONOCIDO");
            datos.put("estadoOperativo", u.getEstadoOperativo().name());
            datos.put("colorEstado", u.getEstadoOperativo().getCodigoColor());
            datos.put("ubicacion", u.getUbicacionActual());
            datos.put("cargaActual", u.getCargaActual());
            datos.put("capacidadMaxima", u.getTipo() != null ? u.getTipo().getCapacidadMaxima() : 0);
            datos.put("tipoAveria", tipoAveriaActiva(u.getCodigo()));
            return datos;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerPedidosMap() {
        return pedidosActivos.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("codigo", p.getCodigo());
            map.put("cliente", p.getCliente() != null ? p.getCliente().getNombre() : "Desconocido");
            map.put("cantidad", p.getCantidadUnidades());
            map.put("destino", p.getDestino());
            map.put("estado", p.getEstado() != null ? p.getEstado().name() : "REGISTRADO");
            map.put("criticidad", p.getNivelCriticidad() != null ? p.getNivelCriticidad().name() : "VERDE");
            map.put("colorCriticidad", p.getNivelCriticidad() != null ? p.getNivelCriticidad().getCodigoColor() : "#28A745");
            map.put("plazoLimite", p.getPlazoLimiteEntrega());
            return map;
        }).collect(Collectors.toList());
    }

    public List<UnidadTransporteDTO> obtenerUnidadesDTO(String tipo, EstadoOperativo estadoFiltro) {
        return unidades.stream()
                .filter(u -> tipo == null || (u.getTipo() != null && u.getTipo().getNombre().equalsIgnoreCase(tipo)))
                .filter(u -> estadoFiltro == null || u.getEstadoOperativo() == estadoFiltro)
                .map(u -> {
                    int capMax = u.getTipo() != null ? u.getTipo().getCapacidadMaxima() : 0;
                    double pctCarga = (capMax > 0) ? (u.getCargaActual() * 100.0) / capMax : 0.0;
                    return UnidadTransporteDTO.builder()
                            .codigo(u.getCodigo())
                            .tipoNombre(u.getTipo() != null ? u.getTipo().getNombre() : null)
                            .capacidadMaxima(capMax)
                            .cargaActual(u.getCargaActual())
                            .capacidadDisponible(u.capacidadDisponible())
                            .porcentajeCarga(Math.round(pctCarga * 10.0) / 10.0)
                            .velocidadPromedioKmH(u.getTipo() != null ? u.getTipo().getVelocidadPromedioKmH() : 0.0)
                            .costoPorKm(u.getTipo() != null ? u.getTipo().getCostoPorKm() : 0.0)
                            .estadoOperativo(u.getEstadoOperativo())
                            .colorEstado(u.getEstadoOperativo().getCodigoColor())
                            .ubicacionActual(u.getUbicacionActual())
                            .nombreConductor("Conductor " + u.getCodigo())
                            .activo(u.isActivo())
                            .tipoAveria(tipoAveriaActiva(u.getCodigo()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<RutaDTO> obtenerRutasDTO(EstadoRuta estadoFiltro, RedVialService redVialService) {
        LocalDateTime instante = reloj.getInstanteActual();
        List<Almacen> almacenes = (almacenRepo != null) ? almacenRepo.findAll() : List.of();
        return rutas.stream()
                .filter(r -> estadoFiltro == null || r.getEstado() == estadoFiltro)
                .map(r -> {
                    // Regreso al almacén con stock más cercano a la última entrega (FAQ 10)
                    Almacen retorno = Ruta.almacenDeRetorno(r.puntoFinalEntregas(), almacenes);
                    List<ParadaRutaDTO> paradasDTO = r.getParadas().stream().map(p -> ParadaRutaDTO.builder()
                            .id(p.getId())
                            .orden(p.getOrden())
                            .pedidoId(p.getPedido() != null ? p.getPedido().getId() : null)
                            .codigoPedido(p.getPedido() != null ? p.getPedido().getCodigo() : null)
                            .nombreCliente(p.getPedido() != null && p.getPedido().getCliente() != null ? p.getPedido().getCliente().getNombre() : null)
                            .cantidadUnidades(p.getPedido() != null ? p.getPedido().getCantidadUnidades() : 0)
                            .destino(p.getPedido() != null ? p.getPedido().getDestino() : null)
                            .horaEstimadaLlegada(p.getHoraEstimadaLlegada())
                            .plazoLimiteEntrega(p.getPedido() != null ? p.getPedido().getPlazoLimiteEntrega() : null)
                            .tiempoServicioMin(p.getTiempoServicioMin())
                            .entregada(p.isEntregada())
                            .build()).collect(Collectors.toList());

                    String tipoVehiculo = (r.getUnidadTransporte() != null && r.getUnidadTransporte().getTipo() != null)
                            ? r.getUnidadTransporte().getTipo().getNombre() : null;

                    return RutaDTO.builder()
                            .id(r.getId())
                            .codigo(r.getCodigo())
                            .unidadCodigo(r.getUnidadTransporte() != null ? r.getUnidadTransporte().getCodigo() : null)
                            .tipoVehiculo(tipoVehiculo)
                            .almacenOrigenCodigo(r.getAlmacenOrigen() != null ? r.getAlmacenOrigen().getCodigo() : null)
                            .almacenOrigenNombre(r.getAlmacenOrigen() != null ? r.getAlmacenOrigen().getNombre() : null)
                            .fechaHoraGeneracion(r.getFechaHoraGeneracion())
                            .distanciaTotalKm(r.getDistanciaTotalKm())
                            .tiempoEstimadoMin(r.getTiempoEstimadoMin())
                            .costoTotal(r.getCostoTotal())
                            .estado(r.getEstado())
                            .paradas(paradasDTO)
                            .camino(calcularCamino(r, retorno, redVialService, instante))
                            .almacenRetornoCodigo(retorno != null ? retorno.getCodigo() : null)
                            .horaLlegadaAlmacen(retorno != null ? r.horaLlegadaAlmacen(retorno) : null)
                            .build()
                            .conRescate(r.getRescate());
                })
                .collect(Collectors.toList());
    }

    private List<Ubicacion> calcularCamino(Ruta r, Almacen retorno, RedVialService redVialService, LocalDateTime instante) {
        if (r.getEstado() == EstadoRuta.COMPLETADA || r.getAlmacenOrigen() == null
                || r.getAlmacenOrigen().getUbicacion() == null || redVialService == null) {
            return List.of();
        }
        try {
            List<Ubicacion> puntos = CaminoRuta.puntosDeVisita(r, retorno != null ? retorno.getUbicacion() : null);
            return CaminoRuta.unir(puntos, redVialService, instante);
        } catch (Exception e) {
            return List.of();
        }
    }
}
