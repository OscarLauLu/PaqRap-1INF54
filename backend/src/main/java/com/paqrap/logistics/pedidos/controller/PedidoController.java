package com.paqrap.logistics.pedidos.controller;

import com.paqrap.logistics.common.service.ArchivoStorageService;
import com.paqrap.logistics.pedidos.dto.ActualizarPedidoDTO;
import com.paqrap.logistics.pedidos.dto.CrearPedidoDTO;
import com.paqrap.logistics.pedidos.dto.PedidoDTO;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Gestión de Pedidos", description = "Endpoints para el registro, consulta y ciclo de vida de pedidos (RF-26 a RF-38)")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ArchivoStorageService archivoStorageService;

    @PostMapping("/cargar")
    @Operation(summary = "Cargar archivo de pedidos ya presente en el servidor (RF-26, RF-64)")
    public ResponseEntity<List<PedidoDTO>> cargarArchivoPedidos(@RequestParam String rutaArchivo) {
        return ResponseEntity.ok(pedidoService.cargarArchivoPedidos(rutaArchivo));
    }

    @PostMapping("/subir-archivo")
    @Operation(summary = "Subir un archivo de pedidos desde el navegador y registrar los pedidos (RF-26, RF-64)")
    public ResponseEntity<List<PedidoDTO>> subirArchivoPedidos(@RequestParam("archivo") MultipartFile archivo) throws java.io.IOException {
        String rutaGuardada = archivoStorageService.guardar(archivo, "pedidos");
        return ResponseEntity.ok(pedidoService.cargarArchivoPedidos(rutaGuardada));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo pedido", description = "Valida datos obligatorios, genera código único, calcula holgura y plazo límite (RF-26 a RF-32)")
    public ResponseEntity<PedidoDTO> registrarPedido(@Valid @RequestBody CrearPedidoDTO dto) {
        return new ResponseEntity<>(pedidoService.registrarPedido(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar pedidos con filtros", description = "Permite filtrar por cliente, estado y rango de fechas (RF-37)")
    public ResponseEntity<List<PedidoDTO>> listarPedidos(
            @RequestParam(required = false) String idCliente,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(pedidoService.listarPedidos(idCliente, estado, desde, hasta));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar detalle de un pedido por ID")
    public ResponseEntity<PedidoDTO> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @GetMapping("/{id}/resumen")
    @Operation(summary = "Consultar resumen del pedido previo a confirmación (RF-38)")
    public ResponseEntity<PedidoDTO> obtenerResumen(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerResumenPedido(id));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Listar pedidos pendientes ordenados por holgura (RF-04)")
    public ResponseEntity<List<PedidoDTO>> listarPendientes() {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPendientesOrdenadosPorHolgura());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar pedido", description = "Solo permitido si el pedido se encuentra en estado REGISTRADO (RF-35)")
    public ResponseEntity<PedidoDTO> modificarPedido(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPedidoDTO dto) {
        return ResponseEntity.ok(pedidoService.modificarPedido(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar pedido", description = "Solo permitido si el pedido se encuentra en estado REGISTRADO (RF-36)")
    public ResponseEntity<Void> cancelarPedido(@PathVariable Long id) {
        pedidoService.cancelarPedido(id);
        return ResponseEntity.noContent().build();
    }
}
