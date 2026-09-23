package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.AveriaRepository;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.repository.PedidoRepository;
import com.paqrap.logistics.planificacion.model.Planificador;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.repository.BloqueoRepository;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Motor central de simulación de operaciones logísticas (RF-64 a RF-75).
 * Gobierna el avance del reloj simulado, procesamiento de eventos programados,
 * ejecución de ciclos de planificación y detección de colapso logístico.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotorSimulacion {

    @Getter
    @Setter
    private EstadoEjecucion estado = EstadoEjecucion.CONFIGURADA;

    private final RelojSimulado reloj;
    private final Planificador planificador;
    private final CargadorArchivos cargadorArchivos;
    private final PedidoRepository pedidoRepository;
    private final com.paqrap.logistics.pedidos.repository.ClienteRepository clienteRepository;
    private final UnidadTransporteRepository unidadRepository;
    private final AlmacenRepository almacenRepository;
    private final BloqueoRepository bloqueoRepository;
    private final AveriaRepository averiaRepository;
    private final com.paqrap.logistics.planificacion.repository.RutaRepository rutaRepository;
    private final ResultadoSimulacionRepository resultadoRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Getter
    private ParametrosSimulacion parametros;

    private ScheduledExecutorService executorService;
    private List<Pedido> pedidosProgramados = new ArrayList<>();
    private List<Bloqueo> bloqueosProgramados = new ArrayList<>();
    private List<Averia> averiasProgramadas = new ArrayList<>();

    // Métricas acumuladas en tiempo real
    private int totalPedidosIngresados = 0;
    private int pedidosEnPlazo = 0;
    private int pedidosTarde = 0;
    private double costoAuto = 0.0;
    private double costoMoto = 0.0;
    private double costoBici = 0.0;
    private int entregasAuto = 0;
    private int entregasMoto = 0;
    private int entregasBici = 0;

    private LocalDateTime instanteInicioEjecucionReal;

    /**
     * Configura los parámetros iniciales de la simulación (RF-64).
     */
    public void configurar(ParametrosSimulacion params) {
        if (params == null || !params.validar()) {
            throw new IllegalArgumentException("Parámetros de simulación inválidos.");
        }
        this.parametros = params;
        this.estado = EstadoEjecucion.CONFIGURADA;

        if (params.getArchivoPedidos() != null) {
            this.pedidosProgramados = cargadorArchivos.cargarPedidos(params.getArchivoPedidos());
        }
        if (params.getArchivoBloqueos() != null) {
            this.bloqueosProgramados = cargadorArchivos.cargarBloqueos(params.getArchivoBloqueos());
        }

        log.info("Motor de simulación configurado exitosamente con {} pedidos y {} bloqueos programados.",
                pedidosProgramados.size(), bloqueosProgramados.size());
    }

    /**
     * Inicia la ejecución de uno de los 3 escenarios soportados (RF-65, RF-66, RF-67).
     */
    public ResultadoSimulacion ejecutar(TipoEscenario escenario) {
        if (parametros == null || pedidosProgramados.isEmpty()) {
            // Auto-load default data files if no prior configuration
            ParametrosSimulacion defaultParams = ParametrosSimulacion.builder().build();
            defaultParams.setArchivoPedidos("datos/ventas.v20260909/ventas.202609.txt");
            defaultParams.setArchivoBloqueos("datos/bloqueos/bloqueo.2609.txt");
            configurar(defaultParams);
            log.info("Auto-configurado con archivos por defecto: {} pedidos, {} bloqueos",
                    pedidosProgramados.size(), bloqueosProgramados.size());
        }

        this.estado = EstadoEjecucion.EN_EJECUCION;
        this.instanteInicioEjecucionReal = LocalDateTime.now();

        // Reset metrics for new run
        this.totalPedidosIngresados = 0;
        this.pedidosEnPlazo = 0;
        this.pedidosTarde = 0;
        this.costoAuto = 0.0;
        this.costoMoto = 0.0;
        this.costoBici = 0.0;
        this.entregasAuto = 0;
        this.entregasMoto = 0;
        this.entregasBici = 0;
        LocalDateTime inicioSim = LocalDateTime.of(2026, 9, 1, 0, 0, 0);
        double factorAceleracion;
        switch (escenario) {
            case DIA_A_DIA:
                factorAceleracion = 1.0;
                break;
            case SIMULACION_5D:
                // 5 días = 120 horas = 7200 min simulados. En 30-60 min reales -> ~150x aceleración
                factorAceleracion = 150.0;
                break;
            case COLAPSO_LOGISTICO:
                factorAceleracion = 50.0;
                break;
            default:
                factorAceleracion = 1.0;
        }
        reloj.inicializar(inicioSim, factorAceleracion);

        log.info("Iniciando escenario [{}] con aceleración {}", escenario.getDescripcion(), factorAceleracion);

        // Arrancar ciclo periódico de simulación
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            if (estado != EstadoEjecucion.EN_EJECUCION) return;

            // Avanza según el tiempo real transcurrido, escalado por el factor de aceleración del escenario.
            reloj.avanzar(java.time.Duration.ofMillis(500));
            LocalDateTime instanteActual = reloj.getInstanteActual();

            // --- Transacción 1: Procesar eventos (nuevos pedidos, bloqueos, averías) ---
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    procesarEventos(instanteActual);
                });
            } catch (Exception e) {
                log.error("Error procesando eventos en instante {}: {}", instanteActual, e.getMessage());
            }

            // --- Transacción 2: Movimientos de flota y entregas ---
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    ejecutarMovimientosFlota(instanteActual);
                });
            } catch (Exception e) {
                log.error("Error en movimientos de flota en instante {}: {}", instanteActual, e.getMessage());
            }

            // --- Transacción 3: Planificación de rutas cada hora simulada ---
            try {
                if (instanteActual.getMinute() == 0) {
                    transactionTemplate.executeWithoutResult(status -> {
                        List<Ruta> nuevasRutas = planificador.ejecutarCicloPlanificacion(instanteActual);
                        registrarCostosYRutas(nuevasRutas);
                    });
                }
            } catch (Exception e) {
                log.error("Error en planificación en instante {}: {}", instanteActual, e.getMessage());
            }

            // Recarga diaria de almacenes intermedios a las 23:59:59 (RF-20)
            try {
                LocalTime hora = reloj.horaSimulada();
                if (hora.getHour() == 23 && hora.getMinute() >= 50) {
                    transactionTemplate.executeWithoutResult(status -> {
                        ejecutarRecargaDiaria();
                    });
                }
            } catch (Exception e) {
                log.error("Error en recarga diaria: {}", e.getMessage());
            }

            // Verificar condición de colapso si aplica (RF-73)
            if (escenario == TipoEscenario.COLAPSO_LOGISTICO && detectarColapso()) {
                log.warn("¡COLAPSO LOGÍSTICO DETECTADO en el instante {}!", instanteActual);
                detener();
                this.estado = EstadoEjecucion.DETENIDA_POR_COLAPSO;
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        ResultadoSimulacion inicial = new ResultadoSimulacion();
        inicial.setEscenario(escenario.name());
        inicial.setFechaHoraInicio(inicioSim);
        inicial.setTotalPedidos(pedidosProgramados.size());
        return inicial;
    }

    /**
     * Procesa los eventos programados en o antes del instante temporal simulado (RF-65).
     */
    public void procesarEventos(LocalDateTime instante) {
        if (instante == null) return;

        // 1. Ingreso de nuevos pedidos según hora simulada
        List<Pedido> entrantes = new ArrayList<>();
        for (Pedido p : pedidosProgramados) {
            if (p.getFechaHoraRegistro() != null && !p.getFechaHoraRegistro().isAfter(instante)) {
                entrantes.add(p);
            }
        }
        for (Pedido p : entrantes) {
            try {
                if (p.getCliente() != null) {
                    com.paqrap.logistics.pedidos.model.Cliente existing = clienteRepository.findByIdCliente(p.getCliente().getIdCliente()).orElse(null);
                    if (existing != null) {
                        p.setCliente(existing);
                    } else {
                        // Save the new client first since cascade is removed
                        p.setCliente(clienteRepository.save(p.getCliente()));
                    }
                }
                pedidoRepository.save(p);
                totalPedidosIngresados++;
            } catch (Exception e) {
                log.warn("Error al guardar pedido {}: {}", p.getCodigo(), e.getMessage());
            }
            pedidosProgramados.remove(p);
        }

        // 2. Activación / desactivación de bloqueos (RF-11)
        for (Bloqueo b : bloqueosProgramados) {
            if (b.estaVigente(instante) && !b.isActivo()) {
                b.activar();
                bloqueoRepository.save(b);
                planificador.replanificarPorBloqueo(b);
            } else if (!b.estaVigente(instante) && b.isActivo()) {
                b.desactivar();
                bloqueoRepository.save(b);
            }
        }

        // 3. Activación de averías (RF-14)
        for (Averia a : averiasProgramadas) {
            if (a.getFechaHoraEvento() != null && !a.getFechaHoraEvento().isAfter(instante) && !a.isResuelta()) {
                averiaRepository.save(a);
                planificador.replanificarPorAveria(a);
            }
        }
    }

    private void ejecutarMovimientosFlota(LocalDateTime instante) {
        // Obtenemos las rutas en ejecución o recién planificadas
        List<Ruta> rutasActivas = rutaRepository.findAll();
        for (Ruta r : rutasActivas) {
            if (r.getEstado() == EstadoRuta.COMPLETADA || r.getUnidadTransporte() == null) continue;
            
            // Marcar como en ejecución si estaba planificada
            if (r.getEstado() == EstadoRuta.PLANIFICADA) {
                r.setEstado(EstadoRuta.EN_EJECUCION);
            }

            boolean todasEntregadas = true;
            for (com.paqrap.logistics.planificacion.model.ParadaRuta p : r.getParadas()) {
                if (!p.isEntregada()) {
                    todasEntregadas = false;
                    log.info("Ruta {} Unidad {}: Evaluando parada. Instante={}, HoraEstimadaLlegada={}", 
                            r.getId(), r.getUnidadTransporte().getCodigo(), instante, p.getHoraEstimadaLlegada());
                            
                    if (p.getHoraEstimadaLlegada() != null && !instante.isBefore(p.getHoraEstimadaLlegada())) {
                        log.info("✅ ENTREGANDO pedido {} en instante {}", p.getPedido().getCodigo(), instante);
                        p.registrarEntrega(instante);
                        // Mover el camión a esta posición
                        if (p.getPedido() != null && p.getPedido().getDestino() != null) {
                            r.getUnidadTransporte().setUbicacionActual(new com.paqrap.logistics.redvial.model.Ubicacion(p.getPedido().getDestino().getPosX(), p.getPedido().getDestino().getPosY()));
                            p.getPedido().setEstado(com.paqrap.logistics.pedidos.model.EstadoPedido.ENTREGADO);
                            pedidoRepository.save(p.getPedido());
                            unidadRepository.save(r.getUnidadTransporte());
                            rutaRepository.save(r); // <--- THIS WAS MISSING, ParadaRuta was never persisted!
                        }
                        
                        if (p.getPedido() != null) {
                            if (p.getPedido().getPlazoLimiteEntrega() != null && p.getPedido().getPlazoLimiteEntrega().isBefore(instante)) {
                                pedidosTarde++;
                            } else {
                                pedidosEnPlazo++;
                            }
                        }
                    }
                    // Si no está entregada, cortamos para no entregar la siguiente
                    break;
                }
            }

            // Si todas están entregadas, regresar al almacén y liberar
            if (todasEntregadas && !r.getParadas().isEmpty()) {
                r.setEstado(EstadoRuta.COMPLETADA);
                r.getUnidadTransporte().setUbicacionActual(new com.paqrap.logistics.redvial.model.Ubicacion(27, 14));
                r.getUnidadTransporte().setEstadoOperativo(com.paqrap.logistics.flota.model.EstadoOperativo.DISPONIBLE);
                unidadRepository.save(r.getUnidadTransporte());
            }
            rutaRepository.save(r);
        }
    }

    /**
     * Detecta automáticamente si la flota colapsó (plazos incumplidos > umbral) en el escenario de colapso (RF-73).
     */
    public boolean detectarColapso() {
        LocalDateTime ahora = reloj.getInstanteActual();
        List<Pedido> pendientes = pedidoRepository.findByEstado(EstadoPedido.REGISTRADO);
        long vencidos = pendientes.stream().filter(p -> p.getPlazoLimiteEntrega().isBefore(ahora)).count();

        // Criterio de colapso: si hay más de 10 pedidos vencidos sin atender
        return vencidos >= 10;
    }

    /**
     * Detiene la corrida de la simulación y consolida los resultados (RF-73, RF-74).
     */
    public void detener() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (estado == EstadoEjecucion.EN_EJECUCION) {
            estado = EstadoEjecucion.FINALIZADA;
            construirResultado(TipoEscenario.valueOf(parametros != null ? "DIA_A_DIA" : "SIMULACION_5D")); // Guarda el resultado real al terminar
        }
        log.info("Motor de simulación detenido.");
    }

    private void ejecutarRecargaDiaria() {
        almacenRepository.findAll().forEach(a -> {
            if (a instanceof AlmacenIntermedio) {
                ((AlmacenIntermedio) a).recargarDiario(reloj.getInstanteActual());
                almacenRepository.save(a);
            }
        });
    }

    private void registrarCostosYRutas(List<Ruta> rutas) {
        for (Ruta r : rutas) {
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

    public ResultadoSimulacion construirResultado(TipoEscenario escenario) {
        double pctCumplimiento = (totalPedidosIngresados > 0)
                ? ((double) pedidosEnPlazo / totalPedidosIngresados) * 100.0 : 100.0;

        ResultadoSimulacion res = ResultadoSimulacion.builder()
                .codigo("SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .escenario(escenario.name())
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
                .build();
        res.guardar();
        resultadoRepository.save(res);
        return res;
    }
}
