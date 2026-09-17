package com.paqrap.logistics.planificacion.algoritmo.alns;

import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Ruta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Operador de Destrucción General: Remueve pedidos de forma uniforme y aleatoria (RF-06).
 * Fomenta la diversificación en el espacio de búsqueda.
 */
public class RandomRemovalOperator extends DestroyOperator {

    private final Random random;

    public RandomRemovalOperator() {
        this.random = new Random();
    }

    public RandomRemovalOperator(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void setSemilla(long seed) {
        this.random.setSeed(seed);
    }

    @Override
    public String getNombre() {
        return "RandomRemovalOperator";
    }

    @Override
    public List<Pedido> destruir(PlanSolution solucion, double factorDestruccion) {
        List<Pedido> liberados = new ArrayList<>();
        if (solucion == null || solucion.getRutas().isEmpty()) {
            return liberados;
        }

        // Recolectar todos los pedidos actualmente asignados
        List<Pedido> asignados = new ArrayList<>();
        for (Ruta r : solucion.getRutas()) {
            for (ParadaRuta p : r.getParadas()) {
                if (p.getPedido() != null) {
                    asignados.add(p.getPedido());
                }
            }
        }

        if (asignados.isEmpty()) return liberados;

        int numARemover = Math.max(1, (int) Math.round(asignados.size() * factorDestruccion));
        Collections.shuffle(asignados, random);

        for (int i = 0; i < Math.min(numARemover, asignados.size()); i++) {
            Pedido pedido = asignados.get(i);
            removerPedidoDeSolucion(solucion, pedido);
            liberados.add(pedido);
        }

        return liberados;
    }

    private void removerPedidoDeSolucion(PlanSolution solucion, Pedido pedido) {
        for (Ruta r : solucion.getRutas()) {
            boolean removed = r.getParadas().removeIf(p -> p.getPedido() != null &&
                    (java.util.Objects.equals(p.getPedido().getCodigo(), pedido.getCodigo()) ||
                     (p.getPedido().getId() != null && p.getPedido().getId().equals(pedido.getId()))));
            if (removed) break;
        }
    }
}
