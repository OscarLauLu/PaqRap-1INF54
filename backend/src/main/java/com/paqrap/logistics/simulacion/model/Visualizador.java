package com.paqrap.logistics.simulacion.model;

import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.AveriaRepository;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.repository.PedidoRepository;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.planificacion.repository.RutaRepository;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.repository.BloqueoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestiona y consolida el estado visual del mapa de operaciones en tiempo real (RF-51 a RF-63).
 * Provee datos de unidades, rutas, pedidos con semáforo de criticidad, y tramos bloqueados o con averías.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Visualizador {

    private final UnidadTransporteRepository unidadRepository;
    private final PedidoRepository pedidoRepository;
    private final RutaRepository rutaRepository;
    private final BloqueoRepository bloqueoRepository;
    private final AveriaRepository averiaRepository;
    private final RelojSimulado relojSimulado;

    /**
     * Compila el estado completo del mapa de operaciones para los clientes conectados (RF-51 a RF-54).
     */
    public Map<String, Object> mostrarMapaOperaciones() {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("relojSimulado", relojSimulado.formatoReloj());
        mapa.put("instanteActual", relojSimulado.getInstanteActual());
        mapa.put("unidades", refrescarEstadoUnidades());
        mapa.put("pedidos", obtenerPedidosConSemaforo());
        mapa.put("bloqueos", resaltarBloqueosYAverias().get("bloqueos"));
        mapa.put("averias", resaltarBloqueosYAverias().get("averias"));
        return mapa;
    }

    /**
     * Refresca las posiciones y estado operativo de las unidades de transporte (RF-51, RF-52).
     */
    public List<Map<String, Object>> refrescarEstadoUnidades() {
        return unidadRepository.findAll().stream().map(u -> {
            Map<String, Object> datos = new HashMap<>();
            // "id" ahora expone el codigo (String): UnidadTransporte usa clave natural, ya no Long id.
            datos.put("id", u.getCodigo());
            datos.put("codigo", u.getCodigo());
            datos.put("tipo", u.getTipo() != null ? u.getTipo().getNombre() : "DESCONOCIDO");
            datos.put("estadoOperativo", u.getEstadoOperativo().name());
            datos.put("colorEstado", u.getEstadoOperativo().getCodigoColor());
            datos.put("ubicacion", u.getUbicacionActual());
            datos.put("cargaActual", u.getCargaActual());
            datos.put("capacidadMaxima", u.getTipo() != null ? u.getTipo().getCapacidadMaxima() : 0);
            return datos;
        }).collect(Collectors.toList());
    }

    /**
     * Aplica los rangos de semáforo de criticidad a los pedidos en curso (RF-53, RF-59).
     */
    public void aplicarSemaforoCriticidad(ConfiguracionSemaforo config) {
        List<Pedido> pedidosActivos = pedidoRepository.findByEstadoIn(
                List.of(EstadoPedido.REGISTRADO, EstadoPedido.PLANIFICADO, EstadoPedido.EN_RUTA));
        for (Pedido p : pedidosActivos) {
            p.evaluarCriticidad(config);
            pedidoRepository.save(p);
        }
    }

    public List<Map<String, Object>> obtenerPedidosConSemaforo() {
        List<Pedido> pedidosActivos = pedidoRepository.findByEstadoIn(
                List.of(EstadoPedido.REGISTRADO, EstadoPedido.PLANIFICADO, EstadoPedido.EN_RUTA));
        return pedidosActivos.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("codigo", p.getCodigo());
            map.put("cliente", p.getCliente() != null ? p.getCliente().getNombre() : "Desconocido");
            map.put("cantidad", p.getCantidadUnidades());
            map.put("destino", p.getDestino());
            map.put("estado", p.getEstado().name());
            map.put("criticidad", p.getNivelCriticidad() != null ? p.getNivelCriticidad().name() : "VERDE");
            map.put("colorCriticidad", p.getNivelCriticidad() != null ? p.getNivelCriticidad().getCodigoColor() : "#28A745");
            map.put("plazoLimite", p.getPlazoLimiteEntrega());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Resalta en el mapa la totalidad de tramos viales bloqueados y ubicaciones de unidades averiadas (RF-60, RF-61).
     */
    public Map<String, Object> resaltarBloqueosYAverias() {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("bloqueos", bloqueoRepository.findByActivoTrue());
        resultado.put("averias", averiaRepository.findByResueltaFalse());
        return resultado;
    }

    /**
     * Filtra la visualización del mapa por zona o cuadrante de la ciudad (RF-62).
     */
    public List<Map<String, Object>> filtrarPorZona(String zona) {
        // Cuadrantes de la ciudad 70x50 km
        return refrescarEstadoUnidades();
    }
}
