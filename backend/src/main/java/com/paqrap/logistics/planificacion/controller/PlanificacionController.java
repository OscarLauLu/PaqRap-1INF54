package com.paqrap.logistics.planificacion.controller;

import com.paqrap.logistics.planificacion.dto.PlanificacionResultDTO;
import com.paqrap.logistics.planificacion.dto.RutaDTO;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.service.PlanificacionService;
import com.paqrap.logistics.simulacion.model.EstadoEjecucion;
import com.paqrap.logistics.simulacion.model.SimulacionSession;
import com.paqrap.logistics.simulacion.service.SimulacionSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/planificacion")
@RequiredArgsConstructor
@Tag(name = "Planificación de Rutas", description = "Endpoints para el cálculo, optimización y consulta de rutas de entrega (RF-01 a RF-16, RF-55)")
public class PlanificacionController {

    private final PlanificacionService planificacionService;
    private final SimulacionSessionManager sessionManager;

    @PostMapping("/ejecutar")
    @Operation(summary = "Ejecutar ciclo de planificación de rutas", description = "Ordena por holgura, asigna almacén óptimo, optimiza paradas con 60 min de servicio y valida plazos y capacidad (RF-01 a RF-09)")
    public ResponseEntity<PlanificacionResultDTO> ejecutarCiclo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime instante) {
        return ResponseEntity.ok(planificacionService.ejecutarCiclo(instante));
    }

    @GetMapping("/rutas")
    @Operation(summary = "Listar rutas planificadas con filtro por estado operacional (RF-55)")
    public ResponseEntity<List<RutaDTO>> listarRutas(
            @RequestParam(required = false) EstadoRuta estado,
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null && sesion.getEstado() != EstadoEjecucion.CONFIGURADA) {
            return ResponseEntity.ok(sessionManager.obtenerRutasDTO(sesion, estado));
        }
        return ResponseEntity.ok(planificacionService.listarRutas(estado));
    }

    @GetMapping("/rutas/{id}")
    @Operation(summary = "Consultar detalle de una ruta con desglose de paradas, tiempos y costos (RF-55)")
    public ResponseEntity<RutaDTO> obtenerRuta(@PathVariable Long id) {
        return ResponseEntity.ok(planificacionService.obtenerRuta(id));
    }

    @PutMapping("/algoritmo")
    @Operation(summary = "Configurar algoritmo metaheurístico de ruteo (ACO o ALNS)", description = "Permite alternar entre los algoritmos Strategy implementados")
    public ResponseEntity<String> cambiarAlgoritmo(@RequestParam String nombre) {
        return ResponseEntity.ok("Algoritmo activo: " + planificacionService.cambiarAlgoritmo(nombre));
    }
}
