package com.paqrap.logistics.simulacion.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una sesión aislada de simulación para un dispositivo o cliente específico.
 * Mantiene su propio reloj, estado de ejecución, escenario y métricas individuales.
 */
@Getter
@Setter
public class SimulacionSession {

    private final String sessionId;
    private final RelojSimulado reloj;
    private EstadoEjecucion estado = EstadoEjecucion.CONFIGURADA;
    private TipoEscenario escenarioActual = TipoEscenario.DIA_A_DIA;
    private ParametrosSimulacion parametros;
    private LocalDateTime ultimoAcceso = LocalDateTime.now();

    // Métricas acumuladas en tiempo real de esta sesión
    private int totalPedidosIngresados = 0;
    private int pedidosEnPlazo = 0;
    private int pedidosTarde = 0;
    private double costoAuto = 0.0;
    private double costoMoto = 0.0;
    private double costoBici = 0.0;
    private int entregasAuto = 0;
    private int entregasMoto = 0;
    private int entregasBici = 0;
    private int bloqueosOcurridos = 0;
    private int averiasOcurridas = 0;
    private LocalDateTime instanteColapsoDetectado;
    private Integer volumenPedidosColapsoDetectado;

    public SimulacionSession(String sessionId) {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        this.reloj = new RelojSimulado();
    }

    public void actualizarAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }

    public double getCostoAcumuladoTotal() {
        return costoAuto + costoMoto + costoBici;
    }

    public void reiniciarMetricas() {
        this.totalPedidosIngresados = 0;
        this.pedidosEnPlazo = 0;
        this.pedidosTarde = 0;
        this.costoAuto = 0.0;
        this.costoMoto = 0.0;
        this.costoBici = 0.0;
        this.entregasAuto = 0;
        this.entregasMoto = 0;
        this.entregasBici = 0;
        this.bloqueosOcurridos = 0;
        this.averiasOcurridas = 0;
        this.instanteColapsoDetectado = null;
        this.volumenPedidosColapsoDetectado = null;
    }
}
