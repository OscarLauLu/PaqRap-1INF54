package com.paqrap.logistics.simulacion.service;

import com.paqrap.logistics.almacen.repository.AlmacenRepository;
import com.paqrap.logistics.flota.dto.UnidadTransporteDTO;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import com.paqrap.logistics.flota.service.PlanMantenimientoService;
import com.paqrap.logistics.planificacion.dto.RutaDTO;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.Planificador;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.service.RedVialService;
import com.paqrap.logistics.simulacion.model.CargadorArchivos;
import com.paqrap.logistics.simulacion.model.MotorSimulacion;
import com.paqrap.logistics.simulacion.model.ParametrosSimulacion;
import com.paqrap.logistics.simulacion.model.RelojSimulado;
import com.paqrap.logistics.simulacion.model.ResultadoSimulacion;
import com.paqrap.logistics.simulacion.model.SimulacionSession;
import com.paqrap.logistics.simulacion.model.TipoEscenario;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de sesiones de simulación para aislar corridas concurrentes entre usuarios.
 * Cada usuario/dispositivo (identificado por X-Simulation-Session-Id) posee su propio ciclo,
 * reloj simulado, flota, órdenes y métricas independientes (RF-64 a RF-75).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulacionSessionManager {

    private final MotorSimulacion motorGlobal;
    private final RelojSimulado relojGlobal;
    private final Planificador planificador;
    private final CargadorArchivos cargadorArchivos;
    private final UnidadTransporteRepository unidadRepository;
    private final AlmacenRepository almacenRepository;
    private final RedVial redVial;
    private final RedVialService redVialService;
    private final ResultadoSimulacionRepository resultadoRepository;
    private final PlanMantenimientoService planMantenimiento;

    private final Map<String, SimulacionSession> sesiones = new ConcurrentHashMap<>();

    /**
     * Obtiene o crea la sesión correspondiente al ID proporcionado.
     * Si no se envía ID o es 'global', retorna null para indicar que debe usarse el motor global.
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

    public void configurarSesion(SimulacionSession sesion, ParametrosSimulacion params) {
        if (sesion == null) return;
        sesion.configurar(params, cargadorArchivos);
    }

    public ResultadoSimulacion iniciarSesion(SimulacionSession sesion, TipoEscenario escenario) {
        if (sesion == null) return null;
        return sesion.ejecutar(
                escenario,
                planificador,
                redVial,
                almacenRepository,
                cargadorArchivos,
                unidadRepository,
                resultadoRepository,
                planMantenimiento
        );
    }

    public void detenerSesion(SimulacionSession sesion) {
        if (sesion == null) return;
        sesion.detener(resultadoRepository);
    }

    public void pausarSesion(SimulacionSession sesion) {
        if (sesion == null) return;
        sesion.pausar();
    }

    public void reanudarSesion(SimulacionSession sesion) {
        if (sesion == null) return;
        sesion.reanudar();
    }

    public void cambiarVelocidadSesion(SimulacionSession sesion, double factor) {
        if (sesion == null) return;
        sesion.cambiarVelocidad(factor);
    }

    public Map<String, Object> obtenerMapaOperaciones(SimulacionSession sesion) {
        if (sesion == null) return null;
        return sesion.obtenerMapaOperaciones();
    }

    public List<Map<String, Object>> obtenerUnidadesMap(SimulacionSession sesion) {
        if (sesion == null) return null;
        return sesion.obtenerUnidadesMap();
    }

    public List<Map<String, Object>> obtenerPedidosMap(SimulacionSession sesion) {
        if (sesion == null) return null;
        return sesion.obtenerPedidosMap();
    }

    public List<UnidadTransporteDTO> obtenerUnidadesDTO(SimulacionSession sesion, String tipo, EstadoOperativo estado) {
        if (sesion == null) return null;
        return sesion.obtenerUnidadesDTO(tipo, estado);
    }

    public List<RutaDTO> obtenerRutasDTO(SimulacionSession sesion, EstadoRuta estado) {
        if (sesion == null) return null;
        return sesion.obtenerRutasDTO(estado, redVialService);
    }

    /**
     * Limpia sesiones inactivas por más de 30 minutos deteniendo sus hilos.
     */
    public void limpiarInactivas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(30);
        sesiones.entrySet().removeIf(entry -> {
            boolean inactiva = entry.getValue().getUltimoAcceso().isBefore(limite);
            if (inactiva) {
                log.info("Eliminando y cerrando sesión inactiva: {}", entry.getKey());
                entry.getValue().detener(resultadoRepository);
            }
            return inactiva;
        });
    }

    @PreDestroy
    public void cerrarSesiones() {
        log.info("Cerrando todas las sesiones de simulación activas...");
        sesiones.values().forEach(s -> s.detener(resultadoRepository));
        sesiones.clear();
    }
}
