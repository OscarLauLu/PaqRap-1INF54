package com.paqrap.logistics.acceptance;

import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.planificacion.controller.PlanificacionController;
import com.paqrap.logistics.planificacion.dto.PlanificacionResultDTO;
import com.paqrap.logistics.planificacion.dto.RutaDTO;
import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.service.PlanificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación para el módulo de Planificación y Algoritmos Metaheurísticos (/api/planificacion).
 * Trazabilidad: RF-01 a RF-16, RF-55, CU-3.5 a CU-3.7.
 * Casos: CP-PLA-001 a CP-PLA-034.
 */
@ExtendWith(MockitoExtension.class)
public class PlanificacionAcceptanceTest {

    private MockMvc mockMvc;

    @Mock
    private PlanificacionService planificacionService;

    @InjectMocks
    private PlanificacionController planificacionController;

    private RutaDTO rutaMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(planificacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        rutaMock = RutaDTO.builder()
                .id(1L)
                .codigo("RUT-001")
                .unidadCodigo("TA01")
                .estado(EstadoRuta.PLANIFICADA)
                .costoTotal(160.0)
                .distanciaTotalKm(20.0)
                .paradas(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("CP-PLA-001: Ejecutar ciclo sin instante (200 OK)")
    void testCP_PLA_001_EjecutarCicloSinInstante() throws Exception {
        PlanificacionResultDTO result = PlanificacionResultDTO.builder()
                .rutas(List.of(rutaMock))
                .build();

        when(planificacionService.ejecutarCiclo(null)).thenReturn(result);

        mockMvc.perform(post("/api/planificacion/ejecutar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutas", hasSize(1)))
                .andExpect(jsonPath("$.rutas[0].codigo", is("RUT-001")));
    }

    @Test
    @DisplayName("CP-PLA-002: Ejecutar ciclo con instante válido ISO-8601 (200 OK)")
    void testCP_PLA_002_EjecutarCicloConInstanteValido() throws Exception {
        PlanificacionResultDTO result = PlanificacionResultDTO.builder()
                .rutas(List.of(rutaMock))
                .build();

        when(planificacionService.ejecutarCiclo(any(LocalDateTime.class))).thenReturn(result);

        mockMvc.perform(post("/api/planificacion/ejecutar?instante=2026-10-12T08:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CP-PLA-003: Instante con formato inválido (400 Bad Request)")
    void testCP_PLA_003_InstanteFormatoInvalido() throws Exception {
        mockMvc.perform(post("/api/planificacion/ejecutar?instante=12/10/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PLA-004: Ejecutar sin pedidos pendientes (200 OK con 0 rutas)")
    void testCP_PLA_004_EjecutarSinPedidosPendientes() throws Exception {
        PlanificacionResultDTO result = PlanificacionResultDTO.builder()
                .rutas(Collections.emptyList())
                .build();

        when(planificacionService.ejecutarCiclo(any())).thenReturn(result);

        mockMvc.perform(post("/api/planificacion/ejecutar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutas", empty()));
    }

    @Test
    @DisplayName("CP-PLA-018: Listar rutas sin filtro (200 OK)")
    void testCP_PLA_018_ListarRutasSinFiltro() throws Exception {
        when(planificacionService.listarRutas(null)).thenReturn(List.of(rutaMock));

        mockMvc.perform(get("/api/planificacion/rutas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("RUT-001")));
    }

    @Test
    @DisplayName("CP-PLA-019: Filtrar rutas por estado PLANIFICADA (200 OK)")
    void testCP_PLA_019_FiltrarRutasPlanificada() throws Exception {
        when(planificacionService.listarRutas(EstadoRuta.PLANIFICADA)).thenReturn(List.of(rutaMock));

        mockMvc.perform(get("/api/planificacion/rutas?estado=PLANIFICADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado", is("PLANIFICADA")));
    }

    @Test
    @DisplayName("CP-PLA-023: Estado de ruta fuera de catálogo (400 Bad Request)")
    void testCP_PLA_023_EstadoRutaFueraDeCatalogo() throws Exception {
        mockMvc.perform(get("/api/planificacion/rutas?estado=VOLANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PLA-024: Detalle de ruta existente (200 OK)")
    void testCP_PLA_024_DetalleRutaExistente() throws Exception {
        when(planificacionService.obtenerRuta(1L)).thenReturn(rutaMock);

        mockMvc.perform(get("/api/planificacion/rutas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("RUT-001")))
                .andExpect(jsonPath("$.costoTotal", is(160.0)));
    }

    @Test
    @DisplayName("CP-PLA-025: Detalle de ruta inexistente (404 Not Found)")
    void testCP_PLA_025_DetalleRutaInexistente() throws Exception {
        when(planificacionService.obtenerRuta(999999L))
                .thenThrow(new ResourceNotFoundException("Ruta", "id", 999999L));

        mockMvc.perform(get("/api/planificacion/rutas/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-PLA-028: Seleccionar algoritmo ACO (200 OK)")
    void testCP_PLA_028_SeleccionarAlgoritmoACO() throws Exception {
        when(planificacionService.cambiarAlgoritmo("ACO")).thenReturn("ACO");

        mockMvc.perform(put("/api/planificacion/algoritmo?nombre=ACO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ACO")));
    }

    @Test
    @DisplayName("CP-PLA-030: Algoritmo no soportado (400 Bad Request)")
    void testCP_PLA_030_AlgoritmoNoSoportado() throws Exception {
        when(planificacionService.cambiarAlgoritmo("GA"))
                .thenThrow(new IllegalArgumentException("Algoritmo no soportado: GA"));

        mockMvc.perform(put("/api/planificacion/algoritmo?nombre=GA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PLA-031: Algoritmo ausente (400 Bad Request)")
    void testCP_PLA_031_AlgoritmoAusente() throws Exception {
        mockMvc.perform(put("/api/planificacion/algoritmo"))
                .andExpect(status().isBadRequest());
    }
}
