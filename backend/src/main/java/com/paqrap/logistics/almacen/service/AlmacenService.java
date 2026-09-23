package com.paqrap.logistics.almacen.service;

import com.paqrap.logistics.almacen.dto.AlmacenDTO;
import com.paqrap.logistics.almacen.dto.MovimientoInventarioDTO;
import com.paqrap.logistics.almacen.model.Almacen;
import com.paqrap.logistics.almacen.model.AlmacenCentral;
import com.paqrap.logistics.almacen.model.AlmacenIntermedio;
import com.paqrap.logistics.almacen.model.MovimientoInventario;
import com.paqrap.logistics.almacen.model.TipoMovimiento;
import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.almacen.repository.MovimientoInventarioRepository;
import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.simulacion.model.Alerta;
import com.paqrap.logistics.simulacion.model.TipoAlerta;
import com.paqrap.logistics.simulacion.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la Gestión de Almacenes e Inventarios (RF-17 a RF-25).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlmacenService {

    private final AlmacenRepository almacenRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final AlertaRepository alertaRepository;

    @Transactional(readOnly = true)
    public List<AlmacenDTO> listarAlmacenes() {
        return almacenRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlmacenDTO obtenerAlmacen(String id) {
        Almacen a = almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen", "id", id));
        return mapearADTO(a);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> obtenerHistorial(String almacenId, int dias) {
        LocalDateTime corte = LocalDateTime.now().minusDays(dias > 0 ? dias : 5);
        return movimientoRepository.findByAlmacenCodigoAndFechaHoraBetweenOrderByFechaHoraDesc(
                almacenId, corte, LocalDateTime.now()).stream()
                .map(this::mapearMovimientoADTO)
                .collect(Collectors.toList());
    }

    /**
     * Ejecuta la recarga diaria automática a las 23:59:59 (RF-20).
     */
    @Transactional
    public void ejecutarRecargaDiaria(LocalDateTime instante) {
        log.info("Ejecutando recarga diaria automática en almacenes intermedios (RF-20)...");
        List<Almacen> almacenes = almacenRepository.findAll();
        for (Almacen a : almacenes) {
            if (a instanceof AlmacenIntermedio) {
                AlmacenIntermedio intermedio = (AlmacenIntermedio) a;
                MovimientoInventario mov = intermedio.recargarDiario(instante);
                almacenRepository.save(intermedio);
                movimientoRepository.save(mov);
                log.info("Almacén {} recargado al tope de 1,000 unidades.", intermedio.getNombre());
            }
        }
    }

    /**
     * Carga manual de inventario validando no superar 1,000 unidades en almacén intermedio (RF-19).
     */
    @Transactional
    public MovimientoInventarioDTO cargarInventario(String almacenId, int cantidad) {
        Almacen a = almacenRepository.findById(almacenId)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen", "id", almacenId));

        if (a instanceof AlmacenIntermedio) {
            AlmacenIntermedio inter = (AlmacenIntermedio) a;
            if (inter.getStockActual() + cantidad > inter.getCapacidadMaxima()) {
                throw new BusinessException("Operación rechazada: El stock resultante ("
                        + (inter.getStockActual() + cantidad) + ") excede la capacidad máxima de "
                        + inter.getCapacidadMaxima() + " unidades (RF-19).");
            }
            inter.setStockActual(inter.getStockActual() + cantidad);
            if (inter.superaUmbralAlerta()) {
                emitirAlertaOcupacion(inter);
            }
        }

        MovimientoInventario mov = MovimientoInventario.builder()
                .codigo("CAR-" + UUID.randomUUID().toString().substring(0, 8))
                .almacen(a)
                .rutaCodigo(null)
                .tipo(TipoMovimiento.CARGA)
                .cantidad(cantidad)
                .fechaHora(LocalDateTime.now())
                .descripcion("Carga manual de inventario")
                .build();

        almacenRepository.save(a);
        return mapearMovimientoADTO(movimientoRepository.save(mov));
    }

    private void emitirAlertaOcupacion(AlmacenIntermedio inter) {
        Alerta alerta = Alerta.builder()
                .codigo("ALT-OCU-" + UUID.randomUUID().toString().substring(0, 6))
                .tipo(TipoAlerta.OCUPACION_ALMACEN)
                .mensaje("El almacén " + inter.getNombre() + " superó el umbral del 90% con "
                        + inter.porcentajeOcupacion() + "% de ocupación (RF-25).")
                .fechaHoraGeneracion(LocalDateTime.now())
                .atendida(false)
                .build();
        alertaRepository.save(alerta);
        log.warn("Alerta emitida: ocupación en {} al {}%", inter.getNombre(), inter.porcentajeOcupacion());
    }

    private AlmacenDTO mapearADTO(Almacen a) {
        boolean esCentral = a instanceof AlmacenCentral;
        double pct = 0.0;
        Integer capMax = null;
        boolean alerta = false;

        if (a instanceof AlmacenIntermedio) {
            AlmacenIntermedio inter = (AlmacenIntermedio) a;
            pct = inter.porcentajeOcupacion();
            capMax = inter.getCapacidadMaxima();
            alerta = inter.superaUmbralAlerta();
        }

        return AlmacenDTO.builder()
                .codigo(a.getCodigo())
                .nombre(a.getNombre())
                .tipo(esCentral ? "CENTRAL" : "INTERMEDIO")
                .ubicacion(a.getUbicacion())
                .stockActual(a.getStockActual())
                .capacidadMaxima(capMax)
                .porcentajeOcupacion(pct)
                .superaAlertaOcupacion(alerta)
                .build();
    }

    private MovimientoInventarioDTO mapearMovimientoADTO(MovimientoInventario m) {
        return MovimientoInventarioDTO.builder()
                .codigo(m.getCodigo())
                .almacenCodigo(m.getAlmacen() != null ? m.getAlmacen().getCodigo() : null)
                .almacenNombre(m.getAlmacen() != null ? m.getAlmacen().getNombre() : null)
                .fechaHora(m.getFechaHora())
                .tipo(m.getTipo())
                .cantidad(m.getCantidad())
                .codigoRuta(m.getCodigoRuta())
                .descripcion(m.getDescripcion())
                .build();
    }
}
