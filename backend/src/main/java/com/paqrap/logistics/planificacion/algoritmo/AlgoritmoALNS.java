package com.paqrap.logistics.planificacion.algoritmo;

import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.algoritmo.alns.AveriaDestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.BloqueoDestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.CapacidadAlmacenDestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.CostRemovalOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.DestroyOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.HolguraGreedyRepairOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.PlanSolution;
import com.paqrap.logistics.planificacion.algoritmo.alns.PonderableOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.RandomInsertionRepairOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.RandomRemovalOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.RegretInsertionRepairOperator;
import com.paqrap.logistics.planificacion.algoritmo.alns.RepairOperator;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Ubicacion;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Metaheurístico de Búsqueda Adaptativa de Gran Vecindario (ALNS)
 * sobre la asignación y ruteo multi-vehículo y multi-almacén (RF-06).
 * Incluye los 5 operadores de destrucción descritos en la sección 3.3:
 * - 2 generales: RandomRemovalOperator, CostRemovalOperator
 * - 3 de dominio: CapacidadAlmacenDestroyOperator, BloqueoDestroyOperator, AveriaDestroyOperator
 * Y 4 operadores de reparación con selección adaptativa:
 * - HolguraGreedyRepairOperator (inserción voraz por holgura)
 * - RegretInsertionRepairOperator(k=2) y (k=3) (Ropke & Pisinger, 2006)
 * - RandomInsertionRepairOperator (diversificación)
 * La aceptación se rige por Recocido Simulado (Simulated Annealing).
 * Los pesos de los operadores se actualizan por segmentos de iteraciones (λ=0.3).
 * Puntuación con 3 niveles: σ₁=3.0 (nueva mejor global), σ₂=2.0 (mejor que actual), σ₃=1.0 (aceptada por SA).
 */
@Slf4j
@Getter
@Setter
@Component("algoritmoALNS")
public class AlgoritmoALNS implements AlgoritmoRuteo {

    private final AlmacenRepository almacenRepository;
    private Random random = new Random();

    private double temperaturaInicial = 1000.0;    // T0 = 1000 (más iteraciones de exploración)
    private double enfriamiento = 0.9995;           // c = 0.9995 (~13,800 iteraciones hasta T=0.1)
    private double factorDestruccion = 0.20;        // 20%
    private long limiteMillis = 5000;               // Límite de tiempo en ms por ejecución
    private int segmentSize = 100;                  // Tamaño de segmento para actualización de pesos

    private PlanSolution actual;
    private PlanSolution mejor;
    private double costoUltimaSolucion = 0.0;

    public void setSemilla(long seed) {
        this.random = new Random(seed);
    }

    public AlgoritmoALNS() {
        this.almacenRepository = null;
    }

    public AlgoritmoALNS(AlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    @Override
    public String obtenerNombre() {
        return "Algoritmo ALNS (Adaptive Large Neighborhood Search)";
    }

    @Override
    public double obtenerCostoSolucion() {
        return costoUltimaSolucion;
    }

    @Override
    public void configurarParametros(Map<String, Double> params) {
        if (params == null) return;
        if (params.containsKey("temperaturaInicial")) this.temperaturaInicial = params.get("temperaturaInicial");
        if (params.containsKey("enfriamiento")) this.enfriamiento = params.get("enfriamiento");
        if (params.containsKey("factorDestruccion")) this.factorDestruccion = params.get("factorDestruccion");
        if (params.containsKey("limiteMillis")) this.limiteMillis = params.get("limiteMillis").longValue();
        if (params.containsKey("segmentSize")) this.segmentSize = params.get("segmentSize").intValue();
        if (params.containsKey("semilla")) setSemilla(params.get("semilla").longValue());
    }

    @Override
    public List<Ruta> construirSolucion(List<Pedido> pedidos, List<UnidadTransporte> flota, RedVial red) {
        if (pedidos == null || pedidos.isEmpty() || flota == null || flota.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Ejecutando {} con {} pedidos y {} unidades...", obtenerNombre(), pedidos.size(), flota.size());

        List<Almacen> almacenes = (almacenRepository != null) ? almacenRepository.findAll() : new ArrayList<>();
        if (almacenes.isEmpty()) {
            almacenes = crearAlmacenesPorDefecto();
        }
        LocalDateTime tiempoInicio = pedidos.stream()
                .map(Pedido::getFechaHoraRegistro)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        // 1. Solución inicial constructiva
        this.actual = PlanSolution.asignacionVoraz(pedidos, flota, almacenes, red, tiempoInicio);
        this.mejor = actual.clonar();

        // 2. Instanciar operadores de destrucción y reparación
        List<DestroyOperator> destructores = new ArrayList<>();
        List<RepairOperator> reparadores = new ArrayList<>();

        // Operadores generales
        destructores.add(new RandomRemovalOperator(this.random));
        destructores.add(new CostRemovalOperator());

        // Operadores de dominio
        destructores.add(new CapacidadAlmacenDestroyOperator(almacenes));
        destructores.add(new BloqueoDestroyOperator(red));
        destructores.add(new AveriaDestroyOperator(flota));

        // Operador de reparación voraz por holgura
        reparadores.add(new HolguraGreedyRepairOperator(red, almacenes, flota));
        // Operador de reparación Regret-2 (Ropke & Pisinger)
        reparadores.add(new RegretInsertionRepairOperator(red, almacenes, flota, 2));
        // Operador de reparación Regret-3 (Ropke & Pisinger)
        reparadores.add(new RegretInsertionRepairOperator(red, almacenes, flota, 3));
        // Operador de reparación por inserción aleatoria (diversificación)
        reparadores.add(new RandomInsertionRepairOperator(red, almacenes, flota, this.random));

        // 3. Bucle metaheurístico ALNS con Recocido Simulado
        long inicio = System.currentTimeMillis();
        double temperatura = this.temperaturaInicial;
        int iteracionSegmento = 0;

        while ((System.currentTimeMillis() - inicio) < limiteMillis && temperatura > 0.1) {
            DestroyOperator destructor = seleccionarPorPeso(destructores);
            RepairOperator reparador = seleccionarPorPeso(reparadores);

            PlanSolution candidata = actual.clonar();
            List<Pedido> liberados = destructor.destruir(candidata, factorDestruccion);
            reparador.reparar(candidata, liberados);

            if (candidata.cumpleRestriccionesDuras(flota, almacenes)) {
                double costoActual = actual.costoTotal();
                double costoCandidata = candidata.costoTotal();

                if (costoCandidata < mejor.costoTotal()) {
                    // σ₁: nueva mejor solución global
                    actual = candidata;
                    mejor = candidata.clonar();
                    destructor.reforzar(3.0);
                    reparador.reforzar(3.0);
                } else if (costoCandidata < costoActual) {
                    // σ₂: mejor que la solución actual (pero no global)
                    actual = candidata;
                    destructor.reforzar(2.0);
                    reparador.reforzar(2.0);
                } else if (aceptar(costoActual, costoCandidata, temperatura)) {
                    // σ₃: peor pero aceptada por Simulated Annealing
                    actual = candidata;
                    destructor.reforzar(1.0);
                    reparador.reforzar(1.0);
                } else {
                    // Rechazada
                    destructor.reforzar(0.1);
                    reparador.reforzar(0.1);
                }
            } else {
                // Infactible
                destructor.reforzar(0.1);
                reparador.reforzar(0.1);
            }

            // Actualización de pesos por segmento (cada segmentSize iteraciones)
            iteracionSegmento++;
            if (iteracionSegmento >= segmentSize) {
                actualizarPesos(destructores, reparadores);
                iteracionSegmento = 0;
            }

            temperatura *= enfriamiento;
        }

        mejor.recalcularMetricas(red, tiempoInicio);
        double costoTotalReal = mejor.getRutas().stream().mapToDouble(Ruta::getCostoTotal).sum();
        this.costoUltimaSolucion = Math.round(costoTotalReal * 100.0) / 100.0;

        log.info("ALNS completado: {} rutas planificadas, costo total S/ {}", mejor.getRutas().size(), costoUltimaSolucion);
        return mejor.getRutas();
    }

    private boolean aceptar(double costoActual, double costoCandidata, double temp) {
        if (costoCandidata < costoActual) return true;
        if (temp <= 0.0) return false;
        return random.nextDouble() < Math.exp((costoActual - costoCandidata) / temp);
    }

    private <T extends PonderableOperator> T seleccionarPorPeso(List<T> operadores) {
        double suma = operadores.stream().mapToDouble(PonderableOperator::getPeso).sum();
        if (suma <= 0.0) return operadores.get(random.nextInt(operadores.size()));
        double rand = random.nextDouble() * suma;
        double acc = 0.0;
        for (T op : operadores) {
            acc += op.getPeso();
            if (acc >= rand) return op;
        }
        return operadores.get(0);
    }

    private void actualizarPesos(List<DestroyOperator> destructores, List<RepairOperator> reparadores) {
        double lambda = 0.3;
        for (PonderableOperator op : destructores) op.actualizarPeso(lambda);
        for (PonderableOperator op : reparadores) op.actualizarPeso(lambda);
    }

    private List<Almacen> crearAlmacenesPorDefecto() {
        List<Almacen> lista = new ArrayList<>();
        AlmacenCentral central = new AlmacenCentral();
        central.setId(1L);
        central.setCodigo("ALM-CEN-01");
        central.setNombre("Almacén Central");
        central.setUbicacion(new Ubicacion(27, 14));
        central.setStockActual(999999);
        lista.add(central);

        AlmacenIntermedio int1 = new AlmacenIntermedio();
        int1.setId(2L);
        int1.setCodigo("ALM-INT-01");
        int1.setNombre("Almacén Intermedio Nor-Oeste");
        int1.setUbicacion(new Ubicacion(12, 38));
        int1.setStockActual(1000);
        int1.setCapacidadMaxima(1000);
        int1.setUmbralAlertaOcupacion(90.0);
        lista.add(int1);

        AlmacenIntermedio int2 = new AlmacenIntermedio();
        int2.setId(3L);
        int2.setCodigo("ALM-INT-02");
        int2.setNombre("Almacén Intermedio Este");
        int2.setUbicacion(new Ubicacion(57, 27));
        int2.setStockActual(1000);
        int2.setCapacidadMaxima(1000);
        int2.setUmbralAlertaOcupacion(90.0);
        lista.add(int2);

        return lista;
    }
}
