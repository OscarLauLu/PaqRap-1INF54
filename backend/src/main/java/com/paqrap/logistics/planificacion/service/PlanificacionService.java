package com.paqrap.logistics.planificacion.service;

import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoACO;
import com.paqrap.logistics.planificacion.algoritmo.AlgoritmoALNS;
import com.paqrap.logistics.planificacion.dto.ParadaRutaDTO;
import com.paqrap.logistics.planificacion.dto.PlanificacionResultDTO;
import com.paqrap.logistics.planificacion.dto.RutaDTO;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.ParadaRuta;
import com.paqrap.logistics.planificacion.model.Planificador;
import com.paqrap.logistics.planificacion.model.Ruta;
import com.paqrap.logistics.planificacion.repository.RutaRepository;
import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.Tramo;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.redvial.service.RedVialService;
import com.paqrap.logistics.simulacion.model.RelojSimulado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la Planificación de Rutas (RF-01 a RF-16, RF-55).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificacionService {

    private final Planificador planificador;
    private final RutaRepository rutaRepository;
    private final AlgoritmoACO algoritmoACO;
    private final AlgoritmoALNS algoritmoALNS;

    // Se usan para armar el camino de cada ruta con los bloqueos vigentes en la simulación
    private final RedVialService redVialService;
    private final RelojSimulado relojSimulado;

    @Transactional
    public PlanificacionResultDTO ejecutarCiclo(LocalDateTime instante) {
        LocalDateTime t = instante != null ? instante : LocalDateTime.now();
        List<Ruta> rutas = planificador.ejecutarCicloPlanificacion(t);

        int totalPedidos = rutas.stream().mapToInt(r -> r.getParadas().size()).sum();
        double costoTotal = rutas.stream().mapToDouble(Ruta::getCostoTotal).sum();

        return PlanificacionResultDTO.builder()
                .algoritmoUtilizado(planificador.getAlgoritmoRuteo().obtenerNombre())
                .instanteEjecucion(t)
                .totalRutasPlanificadas(rutas.size())
                .totalPedidosAtendidos(totalPedidos)
                .costoTotalEstimado(Math.round(costoTotal * 100.0) / 100.0)
                .rutas(rutas.stream().map(this::mapearRutaADTO).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<RutaDTO> listarRutas(EstadoRuta estado) {
        List<Ruta> rutas = (estado != null) ? rutaRepository.findByEstado(estado) : rutaRepository.findAll();
        return rutas.stream().map(this::mapearRutaADTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RutaDTO obtenerRuta(Long id) {
        Ruta r = rutaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta", "id", id));
        return mapearRutaADTO(r);
    }

    public String cambiarAlgoritmo(String nombre) {
        if ("ALNS".equalsIgnoreCase(nombre)) {
            planificador.setAlgoritmoRuteo(algoritmoALNS);
        } else {
            planificador.setAlgoritmoRuteo(algoritmoACO);
        }
        log.info("Algoritmo de ruteo configurado a: {}", planificador.getAlgoritmoRuteo().obtenerNombre());
        return planificador.getAlgoritmoRuteo().obtenerNombre();
    }

    private RutaDTO mapearRutaADTO(Ruta r) {
        List<ParadaRutaDTO> paradasDTO = r.getParadas().stream().map(this::mapearParadaADTO).collect(Collectors.toList());

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
                .camino(calcularCamino(r))
                .build();
    }

    /**
     * Arma el camino de la ruta uniendo el almacén y cada parada con la ruta mínima de la red vial,
     * porque es el cálculo que esquiva bloqueos y así el dibujo no atraviesa calles cerradas.
     * Las rutas completadas se omiten porque ya no se dibujan y solo gastarían cálculo.
     */
    private List<Ubicacion> calcularCamino(Ruta r) {
        if (r.getEstado() == EstadoRuta.COMPLETADA || r.getAlmacenOrigen() == null
                || r.getAlmacenOrigen().getUbicacion() == null) {
            return List.of();
        }
        try {
            // Se usa la hora simulada y no la real, porque los bloqueos dependen del momento de la simulación
            LocalDateTime instante = relojSimulado.getInstanteActual();

            // Orden de visita: el almacén primero y luego las paradas según las ordenó el algoritmo
            List<Ubicacion> puntos = new ArrayList<>();
            puntos.add(r.getAlmacenOrigen().getUbicacion());
            r.getParadas().stream()
                    .sorted(Comparator.comparingInt(ParadaRuta::getOrden))
                    .filter(p -> p.getPedido() != null && p.getPedido().getDestino() != null)
                    .forEach(p -> puntos.add(p.getPedido().getDestino()));

            List<Ubicacion> camino = new ArrayList<>();
            camino.add(puntos.get(0));
            for (int i = 1; i < puntos.size(); i++) {
                Ubicacion desde = puntos.get(i - 1);
                Ubicacion hasta = puntos.get(i);
                List<Tramo> tramos = redVialService.calcularRutaMinima(
                        desde.getPosX(), desde.getPosY(), hasta.getPosX(), hasta.getPosY(), instante);

                if (tramos == null || tramos.isEmpty()) {
                    // Si no hay camino (o es el mismo punto) se une directo para que la línea no quede cortada
                    camino.add(hasta);
                    continue;
                }
                // Se toma el extremo del tramo distinto al punto actual, porque los tramos son de
                // doble sentido y pueden venir con origen y destino invertidos
                Nodo actual = Nodo.deUbicacion(camino.get(camino.size() - 1));
                for (Tramo t : tramos) {
                    Nodo siguiente = t.getNodoOrigen().equals(actual) ? t.getNodoDestino() : t.getNodoOrigen();
                    camino.add(siguiente.aUbicacion());
                    actual = siguiente;
                }
            }
            return camino;
        } catch (Exception e) {
            // Un error aquí no debe impedir listar las rutas; el mapa usará su dibujo de respaldo
            log.warn("No se pudo calcular el camino de la ruta {}: {}", r.getCodigo(), e.getMessage());
            return List.of();
        }
    }

    private ParadaRutaDTO mapearParadaADTO(ParadaRuta p) {
        return ParadaRutaDTO.builder()
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
                .build();
    }
}
