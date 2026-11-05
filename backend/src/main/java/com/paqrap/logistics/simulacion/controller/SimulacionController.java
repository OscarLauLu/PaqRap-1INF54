package com.paqrap.logistics.simulacion.controller;

import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.simulacion.model.MotorSimulacion;
import com.paqrap.logistics.simulacion.model.ParametrosSimulacion;
import com.paqrap.logistics.simulacion.model.RelojSimulado;
import com.paqrap.logistics.simulacion.model.ResultadoSimulacion;
import com.paqrap.logistics.simulacion.model.SimulacionSession;
import com.paqrap.logistics.simulacion.model.TipoEscenario;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import com.paqrap.logistics.simulacion.service.SimulacionSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final SimulacionSessionManager sessionManager;

    @PostMapping("/configurar")
    @Operation(summary = "Configurar parámetros previos de la simulación (RF-64)")
    public ResponseEntity<String> configurar(
            @RequestBody ParametrosSimulacion params,
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerOCrearSesion(sessionId);
        if (sesion != null) {
            sessionManager.configurarSesion(sesion, params);
            return ResponseEntity.ok("Sesión de simulación configurada exitosamente.");
        }
        motorSimulacion.configurar(params);
        return ResponseEntity.ok("Motor de simulación configurado exitosamente.");
    }

    @PostMapping("/iniciar")
    @Operation(summary = "Iniciar corrida de un escenario de simulación (RF-65, RF-66, RF-67)")
    public ResponseEntity<ResultadoSimulacion> iniciar(
            @RequestParam(defaultValue = "DIA_A_DIA") TipoEscenario escenario,
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerOCrearSesion(sessionId);
        if (sesion != null) {
            ResultadoSimulacion res = sessionManager.iniciarSesion(sesion, escenario);
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.ok(motorSimulacion.ejecutar(escenario));
    }

    @PostMapping("/detener")
    @Operation(summary = "Detener la ejecución activa de la simulación")
    public ResponseEntity<String> detener(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sessionManager.detenerSesion(sesion);
            return ResponseEntity.ok("Simulación de sesión detenida.");
        }
        motorSimulacion.detener();
        return ResponseEntity.ok("Simulación detenida.");
    }

    @PostMapping("/pausar")
    @Operation(summary = "Pausar la simulación en curso sin perder su estado")
    public ResponseEntity<String> pausar(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sessionManager.pausarSesion(sesion);
            return ResponseEntity.ok("Simulación de sesión pausada.");
        }
        motorSimulacion.pausar();
        return ResponseEntity.ok("Simulación pausada.");
    }

    @PostMapping("/reanudar")
    @Operation(summary = "Reanudar una simulación pausada")
    public ResponseEntity<String> reanudar(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sessionManager.reanudarSesion(sesion);
            return ResponseEntity.ok("Simulación de sesión reanudada.");
        }
        motorSimulacion.reanudar();
        return ResponseEntity.ok("Simulación reanudada.");
    }

    @PostMapping("/velocidad")
    @Operation(summary = "Cambiar factor de aceleración de la simulación")
    public ResponseEntity<String> cambiarVelocidad(
            @RequestParam double factor,
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            sessionManager.cambiarVelocidadSesion(sesion, factor);
        } else {
            relojSimulado.setFactorAceleracion(factor);
        }
        return ResponseEntity.ok("Velocidad actualizada a " + factor + "x");
    }

    @GetMapping("/reloj")
    @Operation(summary = "Consultar el reloj simulado permanente (día y hora transcurridos) (RF-71)")
    public ResponseEntity<Map<String, Object>> consultarReloj(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            return ResponseEntity.ok(sesion.getRelojMap());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("relojFormateado", relojSimulado.formatoReloj());
        resp.put("diaSimulado", relojSimulado.diaSimulado());
        resp.put("horaSimulada", relojSimulado.horaSimulada());
        resp.put("instanteActual", relojSimulado.getInstanteActual());
        resp.put("instanteInicio", relojSimulado.getInstanteInicio());
        resp.put("factorAceleracion", relojSimulado.getFactorAceleracion());
        resp.put("estadoEjecucion", motorSimulacion.getEstado().name());
        resp.put("escenario", motorSimulacion.getEscenarioActual() != null ? motorSimulacion.getEscenarioActual().name() : null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/metricas")
    @Operation(summary = "Consultar métricas acumuladas en tiempo real de la corrida activa (RF-73, RF-74)",
            description = "Costo acumulado, entregas por tipo de vehículo, incidencias ocurridas y datos del colapso si ya se detectó.")
    public ResponseEntity<Map<String, Object>> consultarMetricas(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        SimulacionSession sesion = sessionManager.obtenerSesion(sessionId);
        if (sesion != null) {
            return ResponseEntity.ok(sesion.getMetricasMap());
        }

        Map<String, Object> resp = new HashMap<>();
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
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/resultados")
    @Operation(summary = "Listar el histórico de resultados de simulaciones ejecutadas (RF-74)")
    public ResponseEntity<List<ResultadoSimulacion>> listarResultados(
            @RequestHeader(value = "X-Simulation-Session-Id", required = false) String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty() && !"global".equalsIgnoreCase(sessionId.trim())) {
            return ResponseEntity.ok(resultadoRepository.findBySessionId(sessionId.trim()));
        }
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
