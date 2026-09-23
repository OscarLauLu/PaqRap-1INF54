package com.paqrap.logistics.almacen.controller;

import com.paqrap.logistics.almacen.dto.AlmacenDTO;
import com.paqrap.logistics.almacen.dto.MovimientoInventarioDTO;
import com.paqrap.logistics.almacen.service.AlmacenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/almacenes")
@RequiredArgsConstructor
@Tag(name = "Almacenes e Inventario", description = "Endpoints para stock de almacenes, recarga diaria y movimientos (RF-17 a RF-25)")
public class AlmacenController {

    private final AlmacenService almacenService;

    @GetMapping
    @Operation(summary = "Listar almacenes con porcentaje de ocupación y alertas (RF-17, RF-18, RF-24, RF-25)")
    public ResponseEntity<List<AlmacenDTO>> listarAlmacenes() {
        return ResponseEntity.ok(almacenService.listarAlmacenes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar detalle de un almacén por código")
    public ResponseEntity<AlmacenDTO> obtenerAlmacen(@PathVariable String id) {
        return ResponseEntity.ok(almacenService.obtenerAlmacen(id));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar historial de movimientos (últimos 5 días simulados por defecto, RF-23)")
    public ResponseEntity<List<MovimientoInventarioDTO>> obtenerHistorial(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int dias) {
        return ResponseEntity.ok(almacenService.obtenerHistorial(id, dias));
    }

    @PostMapping("/{id}/carga")
    @Operation(summary = "Carga manual de inventario validando no superar 1,000 unidades (RF-19)")
    public ResponseEntity<MovimientoInventarioDTO> cargarInventario(
            @PathVariable String id,
            @RequestParam int cantidad) {
        return ResponseEntity.ok(almacenService.cargarInventario(id, cantidad));
    }

    @PostMapping("/recarga-diaria")
    @Operation(summary = "Ejecutar recarga diaria de almacenes intermedios a 1,000 unidades (RF-20)")
    public ResponseEntity<Void> ejecutarRecargaDiaria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime instante) {
        almacenService.ejecutarRecargaDiaria(instante);
        return ResponseEntity.ok().build();
    }
}
