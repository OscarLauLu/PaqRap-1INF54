package com.paqrap.logistics.simulacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Registra y consolida los resultados finales e indicadores de desempeño de una simulación (RF-73, RF-74, RF-75).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "resultados_simulacion")
public class ResultadoSimulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(name = "session_id")
    private String sessionId;

    private String escenario;

    private LocalDateTime fechaHoraInicio;

    private LocalDateTime fechaHoraFin;

    private Long tiempoEjecucionRealSegundos;

    private LocalDateTime instanteColapso;

    private Integer volumenPedidosColapso;

    private int totalPedidos;

    private int pedidosEntregadosEnPlazo;

    private int pedidosEntregadosTarde;

    private double porcentajeCumplimiento;

    private double costoTotalOperacion;

    private double costoTotalAuto;

    private double costoTotalMoto;

    private double costoTotalBicicleta;

    private int entregasAuto;

    private int entregasMoto;

    private int entregasBicicleta;

    private int totalBloqueosOcurridos;

    private int totalAveriasOcurridas;

    public void guardar() {
        if (fechaHoraFin == null) {
            fechaHoraFin = LocalDateTime.now();
        }
        if (fechaHoraInicio != null && tiempoEjecucionRealSegundos == null) {
            tiempoEjecucionRealSegundos = Duration.between(fechaHoraInicio, fechaHoraFin).getSeconds();
        }
    }

    /**
     * Compara los resultados de dos simulaciones guardadas (RF-75).
     * Devuelve las diferencias porcentuales y absolutas en cumplimiento de plazos y costo total.
     */
    public Map<String, Double> compararCon(ResultadoSimulacion otro) {
        Map<String, Double> comparacion = new HashMap<>();
        if (otro == null) return comparacion;

        comparacion.put("diffCumplimientoPct", this.porcentajeCumplimiento - otro.getPorcentajeCumplimiento());
        comparacion.put("diffCostoTotal", this.costoTotalOperacion - otro.getCostoTotalOperacion());
        comparacion.put("diffTotalPedidos", (double) (this.totalPedidos - otro.getTotalPedidos()));
        comparacion.put("diffEntregadosEnPlazo", (double) (this.pedidosEntregadosEnPlazo - otro.getPedidosEntregadosEnPlazo()));
        return comparacion;
    }
}
