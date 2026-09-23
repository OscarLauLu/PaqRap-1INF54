package com.paqrap.logistics.simulacion.controller;

import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import com.paqrap.logistics.simulacion.model.Alerta;
import com.paqrap.logistics.simulacion.model.TipoAlerta;
import com.paqrap.logistics.simulacion.model.Visualizador;
import com.paqrap.logistics.simulacion.repository.AlertaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mapa")
@RequiredArgsConstructor
@Tag(name = "Monitorización y Mapa en Tiempo Real", description = "Endpoints para el estado del mapa, semáforos, bloqueos y alertas (RF-51 a RF-63)")
public class VisualizadorController {

    private final Visualizador visualizador;
    private final ConfiguracionSemaforo configuracionSemaforo;
    private final AlertaRepository alertaRepository;

    @GetMapping("/operaciones")
    @Operation(summary = "Obtener estado completo del mapa de operaciones (RF-51 a RF-54)", description = "Devuelve posiciones de flota, pedidos con semáforo verde/ámbar/rojo, tramos bloqueados y averías")
    public ResponseEntity<Map<String, Object>> obtenerMapaOperaciones() {
        return ResponseEntity.ok(visualizador.mostrarMapaOperaciones());
    }

    @GetMapping("/unidades")
    @Operation(summary = "Consultar ubicación y estado operativo de las unidades en el mapa (RF-51, RF-52)")
    public ResponseEntity<List<Map<String, Object>>> obtenerUnidades() {
        return ResponseEntity.ok(visualizador.refrescarEstadoUnidades());
    }

    @GetMapping("/pedidos")
    @Operation(summary = "Consultar pedidos en curso con semáforo de criticidad (RF-53)")
    public ResponseEntity<List<Map<String, Object>>> obtenerPedidos() {
        return ResponseEntity.ok(visualizador.obtenerPedidosConSemaforo());
    }

    @GetMapping("/incidencias")
    @Operation(summary = "Consultar tramos viales bloqueados y averías mecánicas activas (RF-60, RF-61)")
    public ResponseEntity<Map<String, Object>> obtenerIncidencias() {
        return ResponseEntity.ok(visualizador.resaltarBloqueosYAverias());
    }

    @GetMapping("/semaforo")
    @Operation(summary = "Consultar los rangos actuales del semáforo de criticidad (RF-59)", description = "Valores vigentes en el backend, en horas, para precargar la pantalla de configuración.")
    public ResponseEntity<Map<String, Object>> obtenerSemaforo() {
        return ResponseEntity.ok(Map.of(
                "horasVerde", configuracionSemaforo.getUmbralVerdeMin().toHours(),
                "horasAmbar", configuracionSemaforo.getUmbralAmbarMin().toHours()
        ));
    }

    @PutMapping("/semaforo")
    @Operation(summary = "Configurar rangos de tiempo del semáforo de criticidad (RF-59)", description = "Aplica los nuevos umbrales en caliente sin reiniciar el sistema")
    public ResponseEntity<String> actualizarSemaforo(
            @RequestParam int horasVerde,
            @RequestParam int horasAmbar) {
        configuracionSemaforo.actualizarRangos(Duration.ofHours(horasVerde), Duration.ofHours(horasAmbar));
        visualizador.aplicarSemaforoCriticidad(configuracionSemaforo);
        return ResponseEntity.ok("Rangos de semáforo actualizados: Verde >= " + horasVerde + "h, Ámbar >= " + horasAmbar + "h");
    }

    @GetMapping("/alertas")
    @Operation(summary = "Listar alertas del panel de control (RF-16, RF-25, RF-45)")
    public ResponseEntity<List<Alerta>> listarAlertas(
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(defaultValue = "true") boolean soloNoAtendidas) {
        if (soloNoAtendidas) {
            return ResponseEntity.ok(alertaRepository.findByAtendidaFalse());
        }
        return ResponseEntity.ok(alertaRepository.findAll());
    }

    @PutMapping("/alertas/{id}/atender")
    @Operation(summary = "Marcar alerta como atendida")
    public ResponseEntity<Void> marcarAlertaAtendida(@PathVariable Long id) {
        alertaRepository.findById(id).ifPresent(a -> {
            a.marcarAtendida();
            alertaRepository.save(a);
        });
        return ResponseEntity.ok().build();
    }
}
