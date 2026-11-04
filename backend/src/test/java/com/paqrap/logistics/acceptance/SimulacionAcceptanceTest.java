package com.paqrap.logistics.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.simulacion.controller.SimulacionController;
import com.paqrap.logistics.simulacion.controller.VisualizadorController;
import com.paqrap.logistics.pedidos.model.ConfiguracionSemaforo;
import com.paqrap.logistics.simulacion.model.*;
import com.paqrap.logistics.simulacion.repository.AlertaRepository;
import com.paqrap.logistics.simulacion.repository.ResultadoSimulacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación para el módulo de Motor de Simulación, Visualizador y Mapa.
 * Trazabilidad: RF-64 a RF-75, RF-51 a RF-63, CU-3.13, CU-3.14 y CU-3.15.
 * Casos: CP-SIM-001 a CP-SIM-054 y CP-MAP-001 a CP-MAP-035.
 */
@ExtendWith(MockitoExtension.class)
public class SimulacionAcceptanceTest {

    private MockMvc mockMvcSimulacion;
    private MockMvc mockMvcMapa;
    private ObjectMapper objectMapper;

    @Mock
    private MotorSimulacion motorSimulacion;

    @Mock
    private RelojSimulado relojSimulado;

    @Mock
    private ResultadoSimulacionRepository resultadoRepository;

    @Mock
    private Visualizador visualizador;

    @Mock
    private ConfiguracionSemaforo configuracionSemaforo;

    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private SimulacionController simulacionController;

    @InjectMocks
    private VisualizadorController visualizadorController;

    @BeforeEach
    void setUp() {
        mockMvcSimulacion = MockMvcBuilders.standaloneSetup(simulacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvcMapa = MockMvcBuilders.standaloneSetup(visualizadorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("CP-SIM-001: Configurar parámetros por defecto (200 OK)")
    void testCP_SIM_001_ConfigurarPorDefecto() throws Exception {
        doNothing().when(motorSimulacion).configurar(any(ParametrosSimulacion.class));

        mockMvcSimulacion.perform(post("/api/simulacion/configurar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("configurado exitosamente")));
    }

    @Test
    @DisplayName("CP-SIM-025: Iniciar escenario por defecto DIA_A_DIA (200 OK)")
    void testCP_SIM_025_IniciarEscenarioPorDefecto() throws Exception {
        ResultadoSimulacion resultado = ResultadoSimulacion.builder()
                .codigo("SIM-001")
                .escenario(TipoEscenario.DIA_A_DIA)
                .build();

        when(motorSimulacion.ejecutar(TipoEscenario.DIA_A_DIA)).thenReturn(resultado);

        mockMvcSimulacion.perform(post("/api/simulacion/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("SIM-001")))
                .andExpect(jsonPath("$.escenario", is("DIA_A_DIA")));
    }

    @Test
    @DisplayName("CP-SIM-027: Iniciar SIMULACION_5D (200 OK)")
    void testCP_SIM_027_IniciarSimulacion5D() throws Exception {
        ResultadoSimulacion resultado = ResultadoSimulacion.builder()
                .codigo("SIM-5D-01")
                .escenario(TipoEscenario.SIMULACION_5D)
                .build();

        when(motorSimulacion.ejecutar(TipoEscenario.SIMULACION_5D)).thenReturn(resultado);

        mockMvcSimulacion.perform(post("/api/simulacion/iniciar?escenario=SIMULACION_5D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escenario", is("SIMULACION_5D")));
    }

    @Test
    @DisplayName("CP-SIM-029: Escenario fuera del enum (400 Bad Request)")
    void testCP_SIM_029_EscenarioInvalido() throws Exception {
        mockMvcSimulacion.perform(post("/api/simulacion/iniciar?escenario=SEMANA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-SIM-032: Detener corrida activa (200 OK)")
    void testCP_SIM_032_DetenerCorridaActiva() throws Exception {
        doNothing().when(motorSimulacion).detener();

        mockMvcSimulacion.perform(post("/api/simulacion/detener"))
                .andExpect(status().isOk())
                .andExpect(content().string("Simulación detenida."));
    }

    @Test
    @DisplayName("CP-SIM-034: Pausar corrida (200 OK)")
    void testCP_SIM_034_PausarCorrida() throws Exception {
        doNothing().when(motorSimulacion).pausar();

        mockMvcSimulacion.perform(post("/api/simulacion/pausar"))
                .andExpect(status().isOk())
                .andExpect(content().string("Simulación pausada."));
    }

    @Test
    @DisplayName("CP-SIM-036: Reanudar corrida pausada (200 OK)")
    void testCP_SIM_036_ReanudarCorrida() throws Exception {
        doNothing().when(motorSimulacion).reanudar();

        mockMvcSimulacion.perform(post("/api/simulacion/reanudar"))
                .andExpect(status().isOk())
                .andExpect(content().string("Simulación reanudada."));
    }

    @Test
    @DisplayName("CP-SIM-038: Consultar reloj simulado en ejecución (200 OK)")
    void testCP_SIM_038_ConsultarReloj() throws Exception {
        when(relojSimulado.formatoReloj()).thenReturn("Día 1, 10:00");
        when(relojSimulado.diaSimulado()).thenReturn(1);
        when(relojSimulado.horaSimulada()).thenReturn("10:00");
        when(relojSimulado.getFactorAceleracion()).thenReturn(120.0);
        when(motorSimulacion.getEstado()).thenReturn(EstadoEjecucion.EN_EJECUCION);

        mockMvcSimulacion.perform(get("/api/simulacion/reloj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSimulado", is(1)))
                .andExpect(jsonPath("$.horaSimulada", is("10:00")))
                .andExpect(jsonPath("$.estadoEjecucion", is("EN_EJECUCION")));
    }

    @Test
    @DisplayName("CP-SIM-041: Métricas de corrida en tiempo real (200 OK)")
    void testCP_SIM_041_MetricasEnTiempoReal() throws Exception {
        when(motorSimulacion.getTotalPedidosIngresados()).thenReturn(50);
        when(motorSimulacion.getPedidosEnPlazo()).thenReturn(48);
        when(motorSimulacion.getPedidosTarde()).thenReturn(2);
        when(motorSimulacion.getCostoAcumuladoTotal()).thenReturn(1200.0);

        mockMvcSimulacion.perform(get("/api/simulacion/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidosIngresados", is(50)))
                .andExpect(jsonPath("$.pedidosEnPlazo", is(48)))
                .andExpect(jsonPath("$.pedidosTarde", is(2)))
                .andExpect(jsonPath("$.costoAcumuladoTotal", is(1200.0)));
    }

    @Test
    @DisplayName("CP-SIM-048: Comparar dos corridas (200 OK)")
    void testCP_SIM_048_CompararCorridas() throws Exception {
        ResultadoSimulacion r1 = ResultadoSimulacion.builder().codigo("SIM-001").porcentajeCumplimiento(98.0).build();
        ResultadoSimulacion r2 = ResultadoSimulacion.builder().codigo("SIM-002").porcentajeCumplimiento(92.0).build();

        when(resultadoRepository.findByCodigo("SIM-001")).thenReturn(Optional.of(r1));
        when(resultadoRepository.findByCodigo("SIM-002")).thenReturn(Optional.of(r2));

        mockMvcSimulacion.perform(get("/api/simulacion/comparar?codigo1=SIM-001&codigo2=SIM-002"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CP-SIM-051: Comparar corrida inexistente (404 Not Found)")
    void testCP_SIM_051_CompararCorridaInexistente() throws Exception {
        when(resultadoRepository.findByCodigo("SIM-999")).thenReturn(Optional.empty());

        mockMvcSimulacion.perform(get("/api/simulacion/comparar?codigo1=SIM-999&codigo2=SIM-001"))
                .andExpect(status().isNotFound());
    }

    // ==================== PRUEBAS DE MAPA Y SEMÁFORO ====================

    @Test
    @DisplayName("CP-MAP-001: Snapshot completo de operaciones (200 OK)")
    void testCP_MAP_001_SnapshotCompleto() throws Exception {
        when(visualizador.mostrarMapaOperaciones()).thenReturn(Map.of("unidades", List.of(), "pedidos", List.of()));

        mockMvcMapa.perform(get("/api/mapa/operaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidades", notNullValue()))
                .andExpect(jsonPath("$.pedidos", notNullValue()));
    }

    @Test
    @DisplayName("CP-MAP-014: Consultar umbrales del semáforo (200 OK)")
    void testCP_MAP_014_ConsultarSemaforo() throws Exception {
        when(configuracionSemaforo.getUmbralVerdeMin()).thenReturn(Duration.ofHours(24));
        when(configuracionSemaforo.getUmbralAmbarMin()).thenReturn(Duration.ofHours(8));

        mockMvcMapa.perform(get("/api/mapa/semaforo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasVerde", is(24)))
                .andExpect(jsonPath("$.horasAmbar", is(8)));
    }

    @Test
    @DisplayName("CP-MAP-015: Actualizar umbrales en caliente (200 OK)")
    void testCP_MAP_015_ActualizarUmbralesSemaforo() throws Exception {
        mockMvcMapa.perform(put("/api/mapa/semaforo?horasVerde=24&horasAmbar=8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("actualizados")));
    }

    @Test
    @DisplayName("CP-MAP-017: horasAmbar igual a horasVerde (400 Bad Request)")
    void testCP_MAP_017_HorasAmbarIgualHorasVerde() throws Exception {
        mockMvcMapa.perform(put("/api/mapa/semaforo?horasVerde=8&horasAmbar=8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-MAP-018: horasAmbar mayor que horasVerde (400 Bad Request)")
    void testCP_MAP_018_HorasAmbarMayorHorasVerde() throws Exception {
        mockMvcMapa.perform(put("/api/mapa/semaforo?horasVerde=4&horasAmbar=10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-MAP-019: Umbral <= 0 (400 Bad Request)")
    void testCP_MAP_019_UmbralNegativoOCero() throws Exception {
        mockMvcMapa.perform(put("/api/mapa/semaforo?horasVerde=0&horasAmbar=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-MAP-023: Listar alertas sin filtros (200 OK)")
    void testCP_MAP_023_ListarAlertasSinFiltros() throws Exception {
        Alerta alerta = Alerta.builder()
                .id(1L)
                .codigo("ALT-001")
                .tipo(TipoAlerta.STOCK_MINIMO)
                .atendida(false)
                .build();

        when(alertaRepository.findByAtendidaFalse()).thenReturn(List.of(alerta));

        mockMvcMapa.perform(get("/api/mapa/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("ALT-001")));
    }

    @Test
    @DisplayName("CP-MAP-032: Atender alerta existente (200 OK)")
    void testCP_MAP_032_AtenderAlertaExistente() throws Exception {
        Alerta alerta = Alerta.builder()
                .id(1L)
                .codigo("ALT-001")
                .atendida(false)
                .build();

        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));

        mockMvcMapa.perform(put("/api/mapa/alertas/1/atender"))
                .andExpect(status().isOk());

        verify(alertaRepository, times(1)).save(alerta);
    }

    @Test
    @DisplayName("CP-MAP-033: Atender alerta inexistente (404 Not Found)")
    void testCP_MAP_033_AtenderAlertaInexistente() throws Exception {
        when(alertaRepository.findById(999999L)).thenReturn(Optional.empty());

        mockMvcMapa.perform(put("/api/mapa/alertas/999999/atender"))
                .andExpect(status().isNotFound());
    }
}
