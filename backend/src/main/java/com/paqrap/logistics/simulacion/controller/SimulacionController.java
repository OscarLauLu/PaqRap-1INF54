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

    @PostMapping("/configurar")
    @Operation(summary = "Configurar parámetros previos de la simulación (RF-64)")
    public ResponseEntity<String> configurar(@RequestBody ParametrosSimulacion params) {
        motorSimulacion.configurar(params);
        return ResponseEntity.ok("Motor de simulación configurado exitosamente.");
    }

    @PostMapping("/iniciar")
    @Operation(summary = "Iniciar corrida de un escenario de simulación (RF-65, RF-66, RF-67)")
    public ResponseEntity<ResultadoSimulacion> iniciar(@RequestParam(defaultValue = "DIA_A_DIA") TipoEscenario escenario) {
        return ResponseEntity.ok(motorSimulacion.ejecutar(escenario));
    }

    @PostMapping("/detener")
    @Operation(summary = "Detener la ejecución activa de la simulación")
    public ResponseEntity<String> detener() {
        motorSimulacion.detener();
        return ResponseEntity.ok("Simulación detenida.");
    }

    @GetMapping("/reloj")
    @Operation(summary = "Consultar el reloj simulado permanente (día y hora transcurridos) (RF-71)")
    public ResponseEntity<Map<String, Object>> consultarReloj() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("relojFormateado", relojSimulado.formatoReloj());
        resp.put("diaSimulado", relojSimulado.diaSimulado());
        resp.put("horaSimulada", relojSimulado.horaSimulada());
        resp.put("instanteActual", relojSimulado.getInstanteActual());
        resp.put("factorAceleracion", relojSimulado.getFactorAceleracion());
        resp.put("estadoEjecucion", motorSimulacion.getEstado().name());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/metricas")
    @Operation(summary = "Consultar métricas acumuladas en tiempo real de la corrida activa (RF-73, RF-74)",
            description = "Costo acumulado, entregas por tipo de vehículo, incidencias ocurridas y datos del colapso si ya se detectó.")
    public ResponseEntity<Map<String, Object>> consultarMetricas() {
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
