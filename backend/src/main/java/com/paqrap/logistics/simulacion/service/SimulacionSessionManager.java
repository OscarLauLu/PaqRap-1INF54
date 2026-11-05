package com.paqrap.logistics.simulacion.service;

import com.paqrap.logistics.simulacion.model.MotorSimulacion;
import com.paqrap.logistics.simulacion.model.RelojSimulado;
import com.paqrap.logistics.simulacion.model.SimulacionSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de sesiones de simulación para aislar corridas entre dispositivos.
 * Cada dispositivo (identificado por X-Simulation-Session-Id o sessionId)
 * posee su propio estado, métricas y avance de reloj si ejecuta simulaciones 5D o colapso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacionSessionManager {

    private final MotorSimulacion motorGlobal;
    private final RelojSimulado relojGlobal;

    private final Map<String, SimulacionSession> sesiones = new ConcurrentHashMap<>();

    /**
     * Obtiene la sesión correspondiente al ID proporcionado.
     * Si no se envía ID o está vacío, o si el ID es 'global', retorna null para indicar que debe usarse el motor global.
     */
    public SimulacionSession obtenerOCrearSesion(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty() || "global".equalsIgnoreCase(sessionId.trim())) {
            return null;
        }
        String idLimpio = sessionId.trim();
        limpiarInactivas();
        return sesiones.computeIfAbsent(idLimpio, id -> {
            log.info("Creando nueva sesión de simulación para dispositivo: {}", id);
            SimulacionSession ses = new SimulacionSession(id);
            // Heredar configuración inicial del reloj global
            if (relojGlobal != null) {
                ses.getReloj().inicializar(relojGlobal.getInstanteInicio(), relojGlobal.getFactorAceleracion());
            }
            return ses;
        });
    }

    public SimulacionSession obtenerSesion(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty() || "global".equalsIgnoreCase(sessionId.trim())) {
            return null;
        }
        SimulacionSession ses = sesiones.get(sessionId.trim());
        if (ses != null) {
            ses.actualizarAcceso();
        }
        return ses;
    }

    /**
     * Limpia sesiones inactivas por más de 30 minutos para evitar saturación de memoria.
     */
    public void limpiarInactivas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);
        sesiones.entrySet().removeIf(entry -> {
            boolean inactiva = entry.getValue().getUltimoAcceso().isBefore(limite);
            if (inactiva) {
                log.info("Eliminando sesión de simulación inactiva: {}", entry.getKey());
            }
            return inactiva;
        });
    }
}
