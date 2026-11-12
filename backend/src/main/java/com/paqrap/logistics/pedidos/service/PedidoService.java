package com.paqrap.logistics.pedidos.service;

import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.pedidos.dto.ActualizarPedidoDTO;
import com.paqrap.logistics.pedidos.dto.CrearPedidoDTO;
import com.paqrap.logistics.pedidos.dto.PedidoDTO;
import com.paqrap.logistics.pedidos.model.Cliente;
import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.Pedido;
import com.paqrap.logistics.pedidos.repository.ClienteRepository;
import com.paqrap.logistics.pedidos.repository.PedidoRepository;
import com.paqrap.logistics.redvial.model.Ubicacion;
import com.paqrap.logistics.simulacion.model.CargadorArchivos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la Gestión de Pedidos (RF-26 a RF-38).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ConfiguracionSemaforo configuracionSemaforo;
    private final CargadorArchivos cargadorArchivos;

    /** Ids por consulta IN al cargar clientes (Postgres admite hasta 32 767 parámetros). */
    private static final int LOTE_CLIENTES = 1000;

    @PersistenceContext
    private EntityManager em;

    /**
     * Carga y persiste pedidos desde un archivo plano (RF-26, RF-64).
     */
    @Transactional
    public List<PedidoDTO> cargarArchivoPedidos(String rutaArchivo) {
        List<Pedido> cargados = cargadorArchivos.cargarPedidos(rutaArchivo);

        // Clientes: una consulta por lote de ids (no una por línea) y un único objeto por cliente.
        // Un archivo grande (miles de líneas) hacía una consulta + un save por pedido, lo que
        // tardaba minutos y se degradaba al crecer la sesión de Hibernate.
        List<String> idsClientes = cargados.stream()
                .map(Pedido::getCliente).filter(Objects::nonNull)
                .map(Cliente::getIdCliente).distinct().collect(Collectors.toList());
        Map<String, Cliente> clientes = new HashMap<>();
        for (int i = 0; i < idsClientes.size(); i += LOTE_CLIENTES) {
            List<String> lote = idsClientes.subList(i, Math.min(i + LOTE_CLIENTES, idsClientes.size()));
            clienteRepository.findAllById(lote).forEach(c -> clientes.put(c.getIdCliente(), c));
        }
        for (Pedido p : cargados) {
            Cliente c = p.getCliente();
            if (c != null) {
                Cliente conocido = clientes.get(c.getIdCliente());
                if (conocido == null) {
                    em.persist(c);
                    clientes.put(c.getIdCliente(), c);
                    conocido = c;
                }
                p.setCliente(conocido);
            }
            p.evaluarCriticidad(configuracionSemaforo);
        }
        List<Pedido> guardados = pedidoRepository.saveAll(cargados);
        log.info("Persistidos y procesados {} pedidos desde {}", guardados.size(), rutaArchivo);
        return guardados.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    /**
     * Registra un nuevo pedido con validación exhaustiva y generación automática de metadatos (RF-26 a RF-32).
     */
    @Transactional
    public PedidoDTO registrarPedido(CrearPedidoDTO dto) {
        log.info("Registrando nuevo pedido para cliente: {}", dto.getIdCliente());

        // Validación de campos obligatorios (RF-26, RF-27, RF-28, RF-32)
        if (dto.getCantidadUnidades() <= 0) {
            throw new BusinessException("La cantidad solicitada debe ser un valor entero positivo mayor a cero");
        }
        if (dto.getDestinoX() < 0 || dto.getDestinoX() > 70 || dto.getDestinoY() < 0 || dto.getDestinoY() > 50) {
            throw new BusinessException("Las coordenadas de entrega deben estar dentro de la retícula vial (0 a 70 km, 0 a 50 km)");
        }

        // Obtener o registrar cliente
        Cliente cliente = clienteRepository.findByIdCliente(dto.getIdCliente())
                .orElseGet(() -> {
                    Cliente nuevo = Cliente.builder()
                            .idCliente(dto.getIdCliente())
                            .nombre(dto.getNombreCliente())
                            .ubicacionEntrega(new Ubicacion(dto.getDestinoX(), dto.getDestinoY()))
                            .build();
                    return clienteRepository.save(nuevo);
                });

        // Generar identificador único e irrepetible (RF-31)
        String codigo = "PED-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        LocalDateTime fechaRegistro = LocalDateTime.now(); // RF-29
        LocalDateTime plazoLimite = fechaRegistro.plusHours(dto.getTipoEntrega().getHorasPlazo()); // RF-30

        Pedido pedido = Pedido.builder()
                .codigo(codigo)
                .cliente(cliente)
                .cantidadUnidades(dto.getCantidadUnidades())
                .destino(new Ubicacion(dto.getDestinoX(), dto.getDestinoY()))
                .tipoEntrega(dto.getTipoEntrega())
                .estado(EstadoPedido.REGISTRADO)
                .fechaHoraRegistro(fechaRegistro)
                .plazoLimiteEntrega(plazoLimite)
                .build();

        pedido.evaluarCriticidad(configuracionSemaforo);
        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido registrado con éxito con código: {}", guardado.getCodigo());

        return mapearADTO(guardado);
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtenerPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));
        return mapearADTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPedidos(String idCliente, EstadoPedido estado, LocalDate desde, LocalDate hasta) {
        return pedidoRepository.findAll().stream()
                .filter(p -> idCliente == null || (p.getCliente() != null && idCliente.equalsIgnoreCase(p.getCliente().getIdCliente())))
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> {
                    if (p.getFechaHoraRegistro() == null) return true;
                    LocalDate f = p.getFechaHoraRegistro().toLocalDate();
                    boolean despues = (desde == null) || !f.isBefore(desde);
                    boolean antes = (hasta == null) || !f.isAfter(hasta);
                    return despues && antes;
                })
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    /**
     * Modifica cantidad, destino o tipo de entrega únicamente si el estado es REGISTRADO (RF-35).
     */
    @Transactional
    public PedidoDTO modificarPedido(Long id, ActualizarPedidoDTO dto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        if (!pedido.esModificable()) {
            throw new BusinessException("No se puede modificar un pedido posterior a su asignación a una ruta (Estado actual: "
                    + pedido.getEstado() + ")");
        }

        if (dto.getCantidadUnidades() != null) {
            if (dto.getCantidadUnidades() <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero");
            }
            pedido.setCantidadUnidades(dto.getCantidadUnidades());
        }

        if (dto.getDestinoX() != null && dto.getDestinoY() != null) {
            pedido.setDestino(new Ubicacion(dto.getDestinoX(), dto.getDestinoY()));
        }

        if (dto.getTipoEntrega() != null) {
            pedido.setTipoEntrega(dto.getTipoEntrega());
            pedido.setPlazoLimiteEntrega(pedido.getFechaHoraRegistro().plusHours(dto.getTipoEntrega().getHorasPlazo()));
        }

        pedido.evaluarCriticidad(configuracionSemaforo);
        return mapearADTO(pedidoRepository.save(pedido));
    }

    /**
     * Cancela un pedido únicamente si está en estado REGISTRADO (RF-36).
     */
    @Transactional
    public void cancelarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        if (!pedido.esModificable()) {
            throw new BusinessException("No se puede cancelar un pedido posterior a su asignación a una ruta (Estado: "
                    + pedido.getEstado() + ")");
        }

        pedido.cancelar();
        pedidoRepository.save(pedido);
        log.info("Pedido {} cancelado exitosamente.", pedido.getCodigo());
    }

    /**
     * Retorna el resumen del pedido antes de confirmar su registro (RF-38).
     */
    @Transactional(readOnly = true)
    public PedidoDTO obtenerResumenPedido(Long id) {
        return obtenerPedido(id);
    }

    /**
     * Obtiene pedidos pendientes ordenados de menor a mayor holgura (RF-04).
     */
    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerPedidosPendientesOrdenadosPorHolgura() {
        LocalDateTime ahora = LocalDateTime.now();
        return pedidoRepository.findByEstado(EstadoPedido.REGISTRADO).stream()
                .sorted(Comparator.comparing(p -> p.calcularHolgura(ahora)))
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    private PedidoDTO mapearADTO(Pedido p) {
        LocalDateTime ahora = LocalDateTime.now();
        Duration holgura = p.calcularHolgura(ahora);
        double holguraHoras = Math.round((holgura.toMinutes() / 60.0) * 10.0) / 10.0;

        return PedidoDTO.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .clienteId(p.getCliente() != null ? p.getCliente().getIdCliente() : null)
                .nombreCliente(p.getCliente() != null ? p.getCliente().getNombre() : null)
                .destino(p.getDestino())
                .cantidadUnidades(p.getCantidadUnidades())
                .tipoEntrega(p.getTipoEntrega())
                .estado(p.getEstado())
                .nivelCriticidad(p.getNivelCriticidad())
                .fechaHoraRegistro(p.getFechaHoraRegistro())
                .plazoLimiteEntrega(p.getPlazoLimiteEntrega())
                .fechaHoraEntrega(p.getFechaHoraEntrega())
                .holguraHoras(holguraHoras)
                .unidadAsignadaId(p.getUnidadAsignadaId())
                .rutaAsignadaId(p.getRutaAsignadaId())
                .build();
    }
}
