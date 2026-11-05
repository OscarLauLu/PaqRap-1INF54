package com.paqrap.logistics.simulacion.controller;

import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.simulacion.model.MotorSimulacion;
import com.paqrap.logistics.simulacion.model.ParametrosSimulacion;
import com.paqrap.logistics.simulacion.model.RelojSimulado;
import com.paqrap.logistics.simulacion.model.ResultadoSimulacion;
import com.paqrap.logistics.simulacion.model.TipoEscenario;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulacion")
@RequiredArgsConstructor
@Tag(name = "Simulación y Reportes", description = "Endpoints para la ejecución de escenarios, control del reloj y reporte de KPIs (RF-64 a RF-75)")
public class SimulacionController {

    private final MotorSimulacion motorSimulacion;
    private final RelojSimulado relojSimulado;
    private final ResultadoSimulacionRepository resultadoRepository;
    private final com.paqrap.logistics.simulacion.service.SimulacionSessionManager sessionManager;

    @PostMapping("/configurar")
    @Operation(summary = "Configurar parámetros previos de la simulación (RF-64)")
    public ResponseEntity<String> configurar(
            @RequestBody ParametrosSimulacion params,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerOCrearSesion(sessionId);
        if (sesion != null) {
            sesion.setParametros(params);
            sesion.setEstado(com.paqrap.logistics.simulacion.model.EstadoEjecucion.CONFIGURADA);
            if (params.getFactorAceleracion() != null && params.getFactorAceleracion() > 0) {
                sesion.getReloj().setFactorAceleracion(params.getFactorAceleracion());
            }
            if (params.getFechaHoraInicio() != null) {
                sesion.getReloj().inicializar(params.getFechaHoraInicio(), sesion.getReloj().getFactorAceleracion());
            }
        }
        motorSimulacion.configurar(params);
        return ResponseEntity.ok("Motor de simulación configurado exitosamente.");
    }

    @PostMapping("/iniciar")
    @Operation(summary = "Iniciar corrida de un escenario de simulación (RF-65, RF-66, RF-67)")
    public ResponseEntity<ResultadoSimulacion> iniciar(
            @RequestParam(defaultValue = "DIA_A_DIA") TipoEscenario escenario,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerOCrearSesion(sessionId);
        if (sesion != null) {
            sesion.setEscenarioActual(escenario);
            sesion.setEstado(com.paqrap.logistics.simulacion.model.EstadoEjecucion.EN_EJECUCION);
            sesion.reiniciarMetricas();
            double factor = (sesion.getParametros() != null && sesion.getParametros().getFactorAceleracion() != null && sesion.getParametros().getFactorAceleracion() > 0)
                    ? sesion.getParametros().getFactorAceleracion()
                    : (escenario == TipoEscenario.SIMULACION_5D ? 150.0 : (escenario == TipoEscenario.COLAPSO_LOGISTICO ? 50.0 : 1.0));
            java.time.LocalDateTime inicio = (sesion.getParametros() != null && sesion.getParametros().getFechaHoraInicio() != null)
                    ? sesion.getParametros().getFechaHoraInicio()
                    : java.time.LocalDateTime.of(2026, 9, 1, 0, 0, 0);
            sesion.getReloj().inicializar(inicio, factor);
        }
        return ResponseEntity.ok(motorSimulacion.ejecutar(escenario));
    }

    @PostMapping("/detener")
    @Operation(summary = "Detener la ejecución activa de la simulación")
    public ResponseEntity<String> detener(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sesion.setEstado(com.paqrap.logistics.simulacion.model.EstadoEjecucion.FINALIZADA);
        }
        motorSimulacion.detener();
        return ResponseEntity.ok("Simulación detenida.");
    }

    @PostMapping("/pausar")
    @Operation(summary = "Pausar la simulación en curso sin perder su estado")
    public ResponseEntity<String> pausar(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sesion.setEstado(com.paqrap.logistics.simulacion.model.EstadoEjecucion.PAUSADA);
        }
        motorSimulacion.pausar();
        return ResponseEntity.ok("Simulación pausada.");
    }

    @PostMapping("/reanudar")
    @Operation(summary = "Reanudar una simulación pausada")
    public ResponseEntity<String> reanudar(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sesion.setEstado(com.paqrap.logistics.simulacion.model.EstadoEjecucion.EN_EJECUCION);
        }
        motorSimulacion.reanudar();
        return ResponseEntity.ok("Simulación reanudada.");
    }

    @PostMapping("/velocidad")
    @Operation(summary = "Cambiar factor de aceleración de la simulación")
    public ResponseEntity<String> cambiarVelocidad(
            @RequestParam double factor,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sesion.getReloj().setFactorAceleracion(factor);
        } else {
            relojSimulado.setFactorAceleracion(factor);
        }
        return ResponseEntity.ok("Velocidad actualizada a " + factor + "x");
    }

    @GetMapping("/reloj")
    @Operation(summary = "Consultar el reloj simulado permanente (día y hora transcurridos) (RF-71)")
    public ResponseEntity<Map<String, Object>> consultarReloj(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        RelojSimulado reloj = relojSimulado;
        String estado = (sesion != null) ? sesion.getEstado().name() : motorSimulacion.getEstado().name();
        String escenario = (sesion != null && sesion.getEscenarioActual() != null)
                ? sesion.getEscenarioActual().name()
                : (motorSimulacion.getEscenarioActual() != null ? motorSimulacion.getEscenarioActual().name() : null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("relojFormateado", reloj.formatoReloj());
        resp.put("diaSimulado", reloj.diaSimulado());
        resp.put("horaSimulada", reloj.horaSimulada());
        resp.put("instanteActual", reloj.getInstanteActual());
        resp.put("instanteInicio", reloj.getInstanteInicio());
        resp.put("factorAceleracion", reloj.getFactorAceleracion());
        resp.put("estadoEjecucion", estado);
        resp.put("escenario", escenario);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/metricas")
    @Operation(summary = "Consultar métricas acumuladas en tiempo real de la corrida activa (RF-73, RF-74)",
            description = "Costo acumulado, entregas por tipo de vehículo, incidencias ocurridas y datos del colapso si ya se detectó.")
    public ResponseEntity<Map<String, Object>> consultarMetricas(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        com.paqrap.logistics.simulacion.model.SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        Map<String, Object> resp = new HashMap<>();
        if (sesion != null && sesion.getEstado() == com.paqrap.logistics.simulacion.model.EstadoEjecucion.EN_EJECUCION) {
            resp.put("totalPedidosIngresados", sesion.getTotalPedidosIngresados());
            resp.put("pedidosEnPlazo", sesion.getPedidosEnPlazo());
            resp.put("pedidosTarde", sesion.getPedidosTarde());
            resp.put("costoAcumuladoTotal", sesion.getCostoAcumuladoTotal());
            resp.put("costoAuto", sesion.getCostoAuto());
            resp.put("costoMoto", sesion.getCostoMoto());
            resp.put("costoBici", sesion.getCostoBici());
            resp.put("entregasAuto", sesion.getEntregasAuto());
            resp.put("entregasMoto", sesion.getEntregasMoto());
            resp.put("entregasBici", sesion.getEntregasBici());
            resp.put("bloqueosOcurridos", sesion.getBloqueosOcurridos());
            resp.put("averiasOcurridas", sesion.getAveriasOcurridas());
            resp.put("instanteColapso", sesion.getInstanteColapsoDetectado());
            resp.put("volumenPedidosColapso", sesion.getVolumenPedidosColapsoDetectado());
        } else {
            resp.put("totalPedidosIngresados", motorSimulacion.getTotalPedidosIngresados());
            resp.put("pedidosEnPlazo", motorSimulacion.getPedidosEnPlazo());
            resp.put("pedidosTarde", motorSimulacion.getPedidosTarde());
            resp.put("costoAcumuladoTotal", motorSimulacion.getCostoAcumuladoTotal());
            resp.put("costoAuto", motorSimulacion.getCostoAuto());
            resp.put("costoMoto", motorSimulacion.getCostoMoto());
            resp.put("costoBici", motorSimulacion.getCostoBici());
            resp.put("entregasAuto", motorSimulacion.getEntregasAuto());
            resp.put("entregasMoto", motorSimulacion.getEntregasMoto());
            resp.put("entregasBici", motorSimulacion.getEntregasBici());
            resp.put("bloqueosOcurridos", motorSimulacion.getBloqueosOcurridos());
            resp.put("averiasOcurridas", motorSimulacion.getAveriasOcurridas());
            resp.put("instanteColapso", motorSimulacion.getInstanteColapsoDetectado());
            resp.put("volumenPedidosColapso", motorSimulacion.getVolumenPedidosColapsoDetectado());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/resultados")
    @Operation(summary = "Listar el histórico de resultados de simulaciones ejecutadas (RF-74)")
    public ResponseEntity<List<ResultadoSimulacion>> listarResultados() {
        return ResponseEntity.ok(resultadoRepository.findAll());
    }

    @GetMapping("/comparar")
    @Operation(summary = "Comparar dos corridas de simulación guardadas (RF-75)")
    public ResponseEntity<Map<String, Double>> comparar(
            @RequestParam String codigo1,
            @RequestParam String codigo2) {
        ResultadoSimulacion r1 = resultadoRepository.findByCodigo(codigo1)
                .orElseThrow(() -> new ResourceNotFoundException("ResultadoSimulacion", "codigo", codigo1));
        ResultadoSimulacion r2 = resultadoRepository.findByCodigo(codigo2)
                .orElseThrow(() -> new ResourceNotFoundException("ResultadoSimulacion", "codigo", codigo2));

        return ResponseEntity.ok(r1.compararCon(r2));
    }
}
