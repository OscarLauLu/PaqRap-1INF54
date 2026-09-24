package com.paqrap.logistics.flota.controller;

import com.paqrap.logistics.flota.dto.ActualizarTipoVehiculoDTO;
import com.paqrap.logistics.flota.dto.RegistrarAveriaDTO;
import com.paqrap.logistics.flota.dto.UnidadTransporteDTO;
import com.paqrap.logistics.flota.model.AsignacionTurno;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.service.FlotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/flota")
@RequiredArgsConstructor
@Tag(name = "Gestión de Flota", description = "Endpoints de administración de vehículos, conductores, averías y cambios en caliente (RF-39 a RF-50)")
public class FlotaController {

    private final FlotaService flotaService;

    @GetMapping("/unidades")
    @Operation(summary = "Listar unidades de transporte con filtros de tipo y estado")
    public ResponseEntity<List<UnidadTransporteDTO>> listarUnidades(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) EstadoOperativo estado) {
        return ResponseEntity.ok(flotaService.listarUnidades(tipo, estado));
    }

    @GetMapping("/unidades/disponibles")
    @Operation(summary = "Consultar listado de unidades disponibles para planificar con filtro por tipo (RF-48)")
    public ResponseEntity<List<UnidadTransporteDTO>> listarDisponibles(@RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(flotaService.listarUnidadesDisponibles(tipo));
    }

    @GetMapping("/unidades/{id}")
    @Operation(summary = "Consultar carga actual y porcentaje de capacidad de una unidad (RF-49)")
    public ResponseEntity<UnidadTransporteDTO> obtenerUnidad(@PathVariable String id) {
        return ResponseEntity.ok(flotaService.obtenerUnidad(id));
    }

    @PutMapping("/unidades/{id}/estado")
    @Operation(summary = "Actualizar estado operativo de una unidad (RF-42)")
    public ResponseEntity<UnidadTransporteDTO> cambiarEstado(
            @PathVariable String id,
            @RequestParam EstadoOperativo nuevoEstado) {
        return ResponseEntity.ok(flotaService.cambiarEstadoOperativo(id, nuevoEstado));
    }

    @PostMapping("/unidades/{id}/averia")
    @Operation(summary = "Registrar evento de avería de unidad y calcular reincorporación (RF-14, RF-45)")
    public ResponseEntity<Averia> registrarAveria(
            @PathVariable String id,
            @Valid @RequestBody RegistrarAveriaDTO dto) {
        return ResponseEntity.ok(flotaService.registrarAveria(id, dto));
    }

    @PutMapping("/configuracion/{tipoNombre}")
    @Operation(summary = "Actualizar en caliente velocidad, costo o capacidad por tipo de vehículo (RF-40, RF-41, RF-46)")
    public ResponseEntity<TipoVehiculo> actualizarConfiguracion(
            @PathVariable String tipoNombre,
            @RequestBody ActualizarTipoVehiculoDTO dto) {
        return ResponseEntity.ok(flotaService.actualizarConfiguracionTipoVehiculo(tipoNombre, dto));
    }

    @PostMapping("/unidades/{id}/conductor/{conductorId}")
    @Operation(summary = "Asignar conductor validando turno y descanso para alimentación de 60 min (RF-43, RF-44)",
            description = "turnoId es obligatorio desde el remodelado a clave natural: el turno ya no es fijo por conductor (catálogo sembrado en data.sql: 1=Mañana, 2=Tarde, 3=Noche)")
    public ResponseEntity<AsignacionTurno> asignarConductor(
            @PathVariable String id,
            @PathVariable String conductorId,
            @RequestParam Integer turnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicioAlimentacion) {
        return ResponseEntity.ok(flotaService.asignarConductor(id, conductorId, turnoId, fecha, inicioAlimentacion));
    }

    @DeleteMapping("/unidades/{id}")
    @Operation(summary = "Dar de baja o retirar unidad de la flota activa (RF-47)")
    public ResponseEntity<Void> darDeBaja(@PathVariable String id) {
        flotaService.darDeBajaUnidad(id);
        return ResponseEntity.noContent().build();
    }
}
