package com.paqrap.logistics.planificacion.algoritmo;

import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.algoritmo.aco.ConfigACO;
import com.paqrap.logistics.planificacion.algoritmo.aco.Hormiga;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimización por Colonia de Hormigas (ACO) para la construcción de rutas vehiculares multi-parada (RF-06).
 * Cada hormiga decide parada a parada el próximo pedido a atender respetando:
 * - Capacidad máxima del vehículo (Q_k, RF-07).
 * - Plazo comprometido de cada pedido y ventanas de tiempo (RF-08).
 * - Tramos bloqueados en la retícula vial (RF-12) calculados mediante camino mínimo.
 * - Tiempo de servicio de 60 minutos por entrega (RF-09).
 */
@Slf4j
@Getter
@Setter
@Component("algoritmoACO")
public class AlgoritmoACO implements AlgoritmoRuteo {

    private ConfigACO config = new ConfigACO();
    private final Map<String, Map<String, Double>> pheromone = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private double costoUltimaSolucion = 0.0;

    @Override
    public String obtenerNombre() {
        return "Algoritmo ACO (Ant Colony Optimization)";
    }

    @Override
    public double obtenerCostoSolucion() {
        return costoUltimaSolucion;
    }

    @Override
    public void configurarParametros(Map<String, Double> params) {
        if (params == null) return;
        if (params.containsKey("numHormigas")) {
            config.setMinAnts(params.get("numHormigas").intValue());
            config.setMaxAnts(Math.max(config.getMaxAnts(), params.get("numHormigas").intValue()));
        }
        if (params.containsKey("minAnts")) config.setMinAnts(params.get("minAnts").intValue());
        if (params.containsKey("maxAnts")) config.setMaxAnts(params.get("maxAnts").intValue());
        if (params.containsKey("numIteraciones")) config.setIterations(params.get("numIteraciones").intValue());
        if (params.containsKey("iteraciones")) config.setIterations(params.get("iteraciones").intValue());
        if (params.containsKey("alfa")) config.setAlpha(params.get("alfa"));
        if (params.containsKey("alpha")) config.setAlpha(params.get("alpha"));
        if (params.containsKey("beta")) config.setBeta(params.get("beta"));
        if (params.containsKey("rhoEvaporacion")) config.setRho(params.get("rhoEvaporacion"));
        if (params.containsKey("rho")) config.setRho(params.get("rho"));
        if (params.containsKey("umbralCriticidadMin")) config.setUmbralCriticidadMin(params.get("umbralCriticidadMin"));
        if (params.containsKey("tau0")) config.setTau0(params.get("tau0"));
        if (params.containsKey("pheromoneMin")) config.setPheromoneMin(params.get("pheromoneMin"));
        if (params.containsKey("semilla")) setSemilla(params.get("semilla").longValue());
    }

    public void setSemilla(long seed) {
        this.random.setSeed(seed);
    }

    @Override
    public List<Ruta> construirSolucion(List<Pedido> pedidos, List<UnidadTransporte> flota, RedVial red) {
        if (pedidos == null || pedidos.isEmpty() || flota == null || flota.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Ejecutando {} con {} pedidos y {} unidades disponibles...",
                obtenerNombre(), pedidos.size(), flota.size());

        LocalDateTime horaSimulada = pedidos.stream()
                .map(Pedido::getFechaHoraRegistro)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
                
        List<Pedido> pedidosPendientes = new ArrayList<>(pedidos);
        List<Ruta> rutasGeneradas = new ArrayList<>();
        double costoTotalGlobal = 0.0;

        // Mantener el estado de tiempo y ubicación de cada vehículo (permitir re-uso en 5 días)
        Map<UnidadTransporte, LocalDateTime> vehiculoDisponibleDesde = new HashMap<>();
        Map<UnidadTransporte, Nodo> vehiculoUbicacionActual = new HashMap<>();
        
        for (UnidadTransporte v : flota) {
            vehiculoDisponibleDesde.put(v, horaSimulada);
            Ubicacion ubicacionVehiculo = v.getUbicacionActual() != null ? v.getUbicacionActual() : new Ubicacion(27, 14);
            Nodo nodo = red.obtenerNodo(ubicacionVehiculo.getPosX(), ubicacionVehiculo.getPosY());
            if (nodo == null) nodo = new Nodo(27, 14);
            vehiculoUbicacionActual.put(v, nodo);
        }

        while (!pedidosPendientes.isEmpty()) {
            boolean huboProgreso = false;
            
            // Ordenar flota para despachar primero al vehículo que se libera antes.
            // En caso de empate (ej: inicio del día), priorizar los de MENOR capacidad (Bicis y Motos)
            // para que los Autos grandes no acaparen todos los pedidos pequeños cercanos.
            flota.sort((v1, v2) -> {
                int cmpTime = vehiculoDisponibleDesde.get(v1).compareTo(vehiculoDisponibleDesde.get(v2));
                if (cmpTime != 0) return cmpTime;
                int cap1 = (v1.getTipo() != null) ? v1.getTipo().getCapacidadMaxima() : 24;
                int cap2 = (v2.getTipo() != null) ? v2.getTipo().getCapacidadMaxima() : 24;
                return Integer.compare(cap1, cap2);
            });

            for (UnidadTransporte vehiculo : flota) {
                if (pedidosPendientes.isEmpty()) break;
                if (!vehiculo.isActivo() || (vehiculo.getEstadoOperativo() != null && vehiculo.getEstadoOperativo() != EstadoOperativo.DISPONIBLE)) {
                    continue;
                }

                LocalDateTime tiempoInicio = vehiculoDisponibleDesde.get(vehiculo);
                Nodo nodoOrigen = vehiculoUbicacionActual.get(vehiculo);

                // Construir la mejor ruta para este vehículo usando ACO
                Ruta rutaVehiculo = runAcoParaVehiculo(vehiculo, nodoOrigen, pedidosPendientes, red, tiempoInicio);
                
                if (rutaVehiculo != null && !rutaVehiculo.getParadas().isEmpty()) {
                    rutasGeneradas.add(rutaVehiculo);
                    costoTotalGlobal += rutaVehiculo.getCostoTotal();
                    huboProgreso = true;

                    // Remover pedidos atendidos por esta unidad
                    for (ParadaRuta parada : rutaVehiculo.getParadas()) {
                        pedidosPendientes.remove(parada.getPedido());
                    }

                    // Actualizar el tiempo de disponibilidad (vuelve al almacén o termina su ruta)
                    LocalDateTime nuevoTiempo = tiempoInicio.plusMinutes(rutaVehiculo.getTiempoEstimadoMin());
                    vehiculoDisponibleDesde.put(vehiculo, nuevoTiempo);
                }
            }
            
            if (!huboProgreso) {
                // Evitar bucle infinito si ningún vehículo puede armar rutas (ej: todos bloqueados)
                log.warn("ACO no pudo armar rutas para los pedidos restantes ({} pendientes). Se aborta la iteración.", pedidosPendientes.size());
                break;
            }
        }

        this.costoUltimaSolucion = Math.round(costoTotalGlobal * 100.0) / 100.0;
        log.info("ACO completado: {} rutas construidas con costo total S/ {}", rutasGeneradas.size(), costoUltimaSolucion);
        return rutasGeneradas;
    }

    /**
     * Optimiza y construye la mejor ruta multi-parada para UN vehículo a partir de pedidos pendientes.
     */
    public Ruta runAcoParaVehiculo(UnidadTransporte vehiculo, Nodo origen, List<Pedido> pedidosPendientes,
                                   RedVial red, LocalDateTime horaInicio) {
        int pedidosCriticos = (int) pedidosPendientes.stream()
                .filter(o -> {
                    Duration h = o.calcularHolgura(horaInicio);
                    return h.toMinutes() <= config.getUmbralCriticidadMin();
                })
                .count();

        int m = Math.min(config.getMaxAnts(), Math.max(config.getMinAnts(), Math.max(1, pedidosCriticos)));

        Hormiga mejorGlobal = null;
        for (int it = 0; it < config.getIterations(); it++) {
            
            // Procesamiento en Paralelo: Multihilo nativo de Java para explotar todos los núcleos (vCPUs)
            final LocalDateTime tInicio = horaInicio;
            List<Hormiga> hormigas = java.util.stream.IntStream.range(0, m)
                    .parallel()
                    .mapToObj(k -> {
                        Hormiga ant = new Hormiga();
                        construirRuta(ant, vehiculo, origen, pedidosPendientes, red, tInicio);
                        return ant;
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Buscar la mejor hormiga de esta ronda
            Hormiga mejorIteracion = null;
            for (Hormiga ant : hormigas) {
                if (ant.cumpleTodosLosPlazos() && esMejor(ant, mejorIteracion)) {
                    mejorIteracion = ant;
                }
            }

            evaporar();
            if (mejorIteracion != null) {
                reforzar(origen, mejorIteracion, red);
            }
            if (esMejor(mejorIteracion, mejorGlobal)) {
                mejorGlobal = mejorIteracion;
            }
            // Depósito elitista: la mejor global también refuerza (Elitist Ant System)
            if (mejorGlobal != null) {
                reforzar(origen, mejorGlobal, red);
            }
        }

        return construirRouteDesdeHormiga(vehiculo, mejorGlobal, horaInicio);
    }

    private void construirRuta(Hormiga ant, UnidadTransporte vehiculo, Nodo origen, List<Pedido> pedidosPendientes,
                               RedVial red, LocalDateTime horaInicio) {
        Nodo current = origen;
        LocalDateTime tiempo = horaInicio;
        double velocidadKmH = (vehiculo.getTipo() != null && vehiculo.getTipo().getVelocidadPromedioKmH() > 0)
                ? vehiculo.getTipo().getVelocidadPromedioKmH() : 40.0;
        int capacidadMax = (vehiculo.getTipo() != null) ? vehiculo.getTipo().getCapacidadMaxima() : 24;
        double costoKm = (vehiculo.getTipo() != null) ? vehiculo.getTipo().getCostoPorKm() : 8.0;

        while (true) {
            // 1. PRE-FILTRADO RÁPIDO (Candidate List)
            List<Pedido> posibles = new ArrayList<>();
            for (Pedido o : pedidosPendientes) {
                if (!ant.yaAtendio(o) && (ant.getCargaAcumulada() + o.getCantidadUnidades() <= capacidadMax)) {
                    posibles.add(o);
                }
            }

            // 2. ORDENAR POR CERCANÍA MANHATTAN (Cálculo ultrarrápido O(1))
            final Nodo currFinal = current;
            posibles.sort((o1, o2) -> {
                Ubicacion d1 = o1.getDestino() != null ? o1.getDestino() : new Ubicacion(27, 14);
                Ubicacion d2 = o2.getDestino() != null ? o2.getDestino() : new Ubicacion(27, 14);
                int dist1 = Math.abs(currFinal.getX() - d1.getPosX()) + Math.abs(currFinal.getY() - d1.getPosY());
                int dist2 = Math.abs(currFinal.getX() - d2.getPosX()) + Math.abs(currFinal.getY() - d2.getPosY());
                return Integer.compare(dist1, dist2);
            });

            List<Pedido> candidatos = new ArrayList<>();
            Map<Pedido, Double> distancias = new HashMap<>();
            Map<Pedido, LocalDateTime> llegadas = new HashMap<>();

            // 3. EVALUAR Y FILTRAR HASTA OBTENER K=20 CANDIDATOS VIABLES (ignorando bloqueados)
            int[][] matrizDistancias = red.distanciasDesde(current, tiempo);
            
            for (Pedido o : posibles) {
                Ubicacion dest = o.getDestino() != null ? o.getDestino() : new Ubicacion(27, 14);
                Nodo nodoCliente = red.obtenerNodo(dest.getPosX(), dest.getPosY());
                if (nodoCliente == null) continue;

                double d;
                if (matrizDistancias == null) {
                    d = red.calcularDistancia(current, nodoCliente);
                } else {
                    int dist = matrizDistancias[dest.getPosX()][dest.getPosY()];
                    if (dist == -1) continue; // No hay ruta libre de bloqueos
                    d = (double) dist;
                }

                double tiempoViajeHoras = d / velocidadKmH;
                long minutosViaje = (long) Math.ceil(tiempoViajeHoras * 60.0);
                LocalDateTime llegada = tiempo.plusMinutes(minutosViaje);

                if (o.getPlazoLimiteEntrega() != null && llegada.isAfter(o.getPlazoLimiteEntrega())) {
                    continue; // ya no cumpliría el plazo comprometido (RF-08)
                }

                candidatos.add(o);
                distancias.put(o, d);
                llegadas.put(o, llegada);
                
                // Detenerse al encontrar K=20 candidatos que SÍ son viables
                if (candidatos.size() >= 20) break;
            }


            if (candidatos.isEmpty()) break;

            Pedido elegido = seleccionarSiguientePedido(current, costoKm, candidatos, distancias, llegadas, red);
            double d = distancias.get(elegido);
            double costoTramo = d * costoKm;
            LocalDateTime llegada = llegadas.get(elegido);

            Ubicacion destElegido = elegido.getDestino() != null ? elegido.getDestino() : new Ubicacion(27, 14);
            Nodo nodoElegido = red.obtenerNodo(destElegido.getPosX(), destElegido.getPosY());

            ant.getPedidosAtendidos().add(elegido);
            ant.getVisitados().add(elegido);
            ant.getHoraLlegada().put(elegido, llegada);
            ant.getTramoHaciaPedido().put(elegido, red.caminoMinimoNodos(current, nodoElegido, tiempo));
            ant.setCargaAcumulada(ant.getCargaAcumulada() + elegido.getCantidadUnidades());
            ant.setCostoAcumulado(ant.getCostoAcumulado() + costoTramo);
            ant.setDistanciaAcumuladaKm(ant.getDistanciaAcumuladaKm() + d);

            current = nodoElegido;
            tiempo = llegada.plusMinutes(60); // 60 min de servicio impactan paradas posteriores (RF-09)
        }
        if (!ant.getPedidosAtendidos().isEmpty()) {
            double dRetorno = red.distanciaMinima(current, origen, tiempo);
            if (dRetorno < Double.MAX_VALUE) {
                ant.setDistanciaAcumuladaKm(ant.getDistanciaAcumuladaKm() + dRetorno);
                ant.setCostoAcumulado(ant.getCostoAcumulado() + (dRetorno * costoKm));
                long minRetorno = (long) Math.ceil((dRetorno / velocidadKmH) * 60.0);
                tiempo = tiempo.plusMinutes(minRetorno);
            }
        }
        ant.setTiempoAcumuladoMin((int) Duration.between(horaInicio, tiempo).toMinutes());
    }

    private Pedido seleccionarSiguientePedido(Nodo current, double costoPorKm, List<Pedido> candidatos,
                                              Map<Pedido, Double> distancias, Map<Pedido, LocalDateTime> llegadas,
                                              RedVial red) {
        double suma = 0.0;
        Map<Pedido, Double> valores = new HashMap<>();

        for (Pedido o : candidatos) {
            LocalDateTime llegada = llegadas.get(o);
            long holguraMin = (o.getPlazoLimiteEntrega() != null)
                    ? Duration.between(llegada, o.getPlazoLimiteEntrega()).toMinutes() : 600;
            double holguraRestante = Math.max(1.0, (double) holguraMin);
            double urgencia = 1.0 / holguraRestante;
            double costoTramo = distancias.get(o) * costoPorKm;
            double heuristica = urgencia / (costoTramo + 1.0);

            Ubicacion dest = o.getDestino() != null ? o.getDestino() : new Ubicacion(27, 14);
            Nodo nodoCliente = red.obtenerNodo(dest.getPosX(), dest.getPosY());
            double feromona = getPheromone(current, nodoCliente);

            double valor = Math.pow(feromona, config.getAlpha()) * Math.pow(heuristica, config.getBeta());
            valores.put(o, valor);
            suma += valor;
        }

        if (suma <= 0.0) return candidatos.get(random.nextInt(candidatos.size()));
        double rand = random.nextDouble() * suma;
        double acc = 0.0;
        for (Pedido o : candidatos) {
            acc += valores.get(o);
            if (acc >= rand) return o;
        }
        return candidatos.get(candidatos.size() - 1);
    }

    private void evaporar() {
        for (Map<String, Double> fila : pheromone.values()) {
            fila.replaceAll((k, v) -> Math.max(config.getPheromoneMin(), v * (1.0 - config.getRho())));
        }
    }

    private void reforzar(Nodo origenNode, Hormiga ant, RedVial red) {
        Nodo current = origenNode;
        double deposito = 1.0 / (ant.getCostoAcumulado() + 1.0);
        for (Pedido o : ant.getPedidosAtendidos()) {
            Ubicacion dest = o.getDestino() != null ? o.getDestino() : new Ubicacion(27, 14);
            Nodo nodoCliente = red.obtenerNodo(dest.getPosX(), dest.getPosY());
            addPheromone(current, nodoCliente, deposito);
            current = nodoCliente;
        }
    }

    private double getPheromone(Nodo a, Nodo b) {
        if (a == null || b == null) return config.getTau0();
        return pheromone.computeIfAbsent(a.getClave(), k -> new ConcurrentHashMap<>()).getOrDefault(b.getClave(), config.getTau0());
    }

    private void addPheromone(Nodo a, Nodo b, double delta) {
        if (a == null || b == null) return;
        pheromone.computeIfAbsent(a.getClave(), k -> new ConcurrentHashMap<>())
                 .merge(b.getClave(), config.getTau0() + delta, (oldVal, newVal) -> oldVal + delta);
    }

    private boolean esMejor(Hormiga candidata, Hormiga actual) {
        if (candidata == null) return false;
        if (actual == null) return true;
        if (candidata.getPedidosAtendidos().size() != actual.getPedidosAtendidos().size()) {
            return candidata.getPedidosAtendidos().size() > actual.getPedidosAtendidos().size();
        }
        return candidata.getCostoAcumulado() < actual.getCostoAcumulado();
    }

    private Ruta construirRouteDesdeHormiga(UnidadTransporte vehiculo, Hormiga ant, LocalDateTime horaInicio) {
        if (ant == null || ant.getPedidosAtendidos().isEmpty()) return null;

        Ruta ruta = Ruta.builder()
                .codigo("RUT-" + UUID.randomUUID().toString().substring(0, 8))
                .unidadTransporte(vehiculo)
                .fechaHoraGeneracion(horaInicio)
                .distanciaTotalKm(Math.round(ant.getDistanciaAcumuladaKm() * 100.0) / 100.0)
                .tiempoEstimadoMin(ant.getTiempoAcumuladoMin())
                .costoTotal(Math.round(ant.getCostoAcumulado() * 100.0) / 100.0)
                .paradas(new ArrayList<>())
                .build();

        for (Pedido o : ant.getPedidosAtendidos()) {
            ParadaRuta parada = ParadaRuta.builder()
                    .pedido(o)
                    .horaEstimadaLlegada(ant.getHoraLlegada().get(o))
                    .tiempoServicioMin(60)
                    .build();
            ruta.agregarParada(parada);
        }
        return ruta;
    }
}
