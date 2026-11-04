package com.paqrap.logistics.acceptance;

import com.paqrap.logistics.almacen.controller.AlmacenController;
import com.paqrap.logistics.almacen.dto.AlmacenDTO;
import com.paqrap.logistics.almacen.dto.MovimientoInventarioDTO;
import com.paqrap.logistics.almacen.model.TipoMovimiento;
import com.paqrap.logistics.almacen.service.AlmacenService;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.redvial.model.Ubicacion;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación para el módulo de Almacenes e Inventario (/api/almacenes).
 * Trazabilidad: RF-17 a RF-25, CU-3.8 y CU-3.9.
 * Casos: CP-ALM-001 a CP-ALM-037.
 */
@ExtendWith(MockitoExtension.class)
public class AlmacenAcceptanceTest {

    private MockMvc mockMvc;

    @Mock
    private AlmacenService almacenService;

    @InjectMocks
    private AlmacenController almacenController;

    private AlmacenDTO alm01Central;
    private AlmacenDTO alm02Intermedio;
    private AlmacenDTO alm03Intermedio;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(almacenController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        alm01Central = AlmacenDTO.builder()
                .codigo("ALM01")
                .nombre("Almacén Central")
                .tipo("CENTRAL")
                .ubicacion(new Ubicacion(25, 15))
                .stockActual(null) // Inventario ilimitado
                .capacidadMaxima(null)
                .porcentajeOcupacion(0.0)
                .superaAlertaOcupacion(false)
                .build();

        alm02Intermedio = AlmacenDTO.builder()
                .codigo("ALM02")
                .nombre("Almacén Nor-Oeste")
                .tipo("INTERMEDIO")
                .ubicacion(new Ubicacion(12, 38))
                .stockActual(400)
                .capacidadMaxima(1000)
                .porcentajeOcupacion(40.0)
                .superaAlertaOcupacion(false)
                .build();

        alm03Intermedio = AlmacenDTO.builder()
                .codigo("ALM03")
                .nombre("Almacén Este")
                .tipo("INTERMEDIO")
                .ubicacion(new Ubicacion(55, 27))
                .stockActual(250)
                .capacidadMaxima(1000)
                .porcentajeOcupacion(25.0)
                .superaAlertaOcupacion(false)
                .build();
    }

    @Test
    @DisplayName("CP-ALM-001: Listar todos los almacenes")
    void testCP_ALM_001_ListarTodosLosAlmacenes() throws Exception {
        when(almacenService.listarAlmacenes()).thenReturn(List.of(alm01Central, alm02Intermedio, alm03Intermedio));

        mockMvc.perform(get("/api/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].codigo", is("ALM01")))
                .andExpect(jsonPath("$[0].tipo", is("CENTRAL")))
                .andExpect(jsonPath("$[1].codigo", is("ALM02")))
                .andExpect(jsonPath("$[1].capacidadMaxima", is(1000)))
                .andExpect(jsonPath("$[1].porcentajeOcupacion", is(40.0)));
    }

    @Test
    @DisplayName("CP-ALM-002: Cálculo del % de ocupación")
    void testCP_ALM_002_CalculoPorcentajeOcupacion() throws Exception {
        when(almacenService.listarAlmacenes()).thenReturn(List.of(alm02Intermedio));

        mockMvc.perform(get("/api/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].porcentajeOcupacion", is(40.0)));
    }

    @Test
    @DisplayName("CP-ALM-003: Alerta de stock mínimo o umbral")
    void testCP_ALM_003_AlertaStockMinimo() throws Exception {
        AlmacenDTO alm02ConAlerta = AlmacenDTO.builder()
                .codigo("ALM02")
                .nombre("Almacén Nor-Oeste")
                .tipo("INTERMEDIO")
                .ubicacion(new Ubicacion(12, 38))
                .stockActual(950)
                .capacidadMaxima(1000)
                .porcentajeOcupacion(95.0)
                .superaAlertaOcupacion(true)
                .build();

        when(almacenService.listarAlmacenes()).thenReturn(List.of(alm02ConAlerta));

        mockMvc.perform(get("/api/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].superaAlertaOcupacion", is(true)));
    }

    @Test
    @DisplayName("CP-ALM-004: Sin alerta con stock normal")
    void testCP_ALM_004_SinAlertaConStockNormal() throws Exception {
        when(almacenService.listarAlmacenes()).thenReturn(List.of(alm03Intermedio));

        mockMvc.perform(get("/api/almacenes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].superaAlertaOcupacion", is(false)));
    }

    @Test
    @DisplayName("CP-ALM-005: Detalle de almacén central")
    void testCP_ALM_005_DetalleAlmacenCentral() throws Exception {
        when(almacenService.obtenerAlmacen("ALM01")).thenReturn(alm01Central);

        mockMvc.perform(get("/api/almacenes/ALM01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("ALM01")))
                .andExpect(jsonPath("$.tipo", is("CENTRAL")))
                .andExpect(jsonPath("$.stockActual", nullValue()));
    }

    @Test
    @DisplayName("CP-ALM-006: Detalle de almacén intermedio")
    void testCP_ALM_006_DetalleAlmacenIntermedio() throws Exception {
        when(almacenService.obtenerAlmacen("ALM02")).thenReturn(alm02Intermedio);

        mockMvc.perform(get("/api/almacenes/ALM02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("ALM02")))
                .andExpect(jsonPath("$.capacidadMaxima", is(1000)))
                .andExpect(jsonPath("$.stockActual", is(400)));
    }

    @Test
    @DisplayName("CP-ALM-007: Almacén inexistente")
    void testCP_ALM_007_AlmacenInexistente() throws Exception {
        when(almacenService.obtenerAlmacen("ALM99"))
                .thenThrow(new ResourceNotFoundException("Almacen", "id", "ALM99"));

        mockMvc.perform(get("/api/almacenes/ALM99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("ALM99")));
    }

    @Test
    @DisplayName("CP-ALM-008: Código con formato inválido")
    void testCP_ALM_008_CodigoInvalido() throws Exception {
        when(almacenService.obtenerAlmacen("ALM@#"))
                .thenThrow(new ResourceNotFoundException("Almacen", "id", "ALM@#"));

        mockMvc.perform(get("/api/almacenes/ALM@#"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-ALM-009: Historial con días por defecto (5 días)")
    void testCP_ALM_009_HistorialDiasPorDefecto() throws Exception {
        MovimientoInventarioDTO mov = MovimientoInventarioDTO.builder()
                .codigo("MOV-01")
                .almacenCodigo("ALM02")
                .cantidad(100)
                .tipo(TipoMovimiento.CARGA)
                .fechaHora(LocalDateTime.now())
                .build();

        when(almacenService.obtenerHistorial("ALM02", 5)).thenReturn(List.of(mov));

        mockMvc.perform(get("/api/almacenes/ALM02/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("MOV-01")));
    }

    @Test
    @DisplayName("CP-ALM-010: Historial de 1 día (límite inferior)")
    void testCP_ALM_010_HistorialUnDia() throws Exception {
        when(almacenService.obtenerHistorial("ALM01", 1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/almacenes/ALM01/historial?dias=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("CP-ALM-012: Historial con dias = 0 (rechazado con 400)")
    void testCP_ALM_012_HistorialDiasCero() throws Exception {
        when(almacenService.obtenerHistorial("ALM02", 0))
                .thenThrow(new IllegalArgumentException("El parámetro 'dias' debe ser mayor a 0."));

        mockMvc.perform(get("/api/almacenes/ALM02/historial?dias=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("CP-ALM-013: Historial con dias negativo (rechazado con 400)")
    void testCP_ALM_013_HistorialDiasNegativo() throws Exception {
        when(almacenService.obtenerHistorial("ALM02", -3))
                .thenThrow(new IllegalArgumentException("El parámetro 'dias' debe ser mayor a 0."));

        mockMvc.perform(get("/api/almacenes/ALM02/historial?dias=-3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-014: Historial con dias no numérico (400 Bad Request)")
    void testCP_ALM_014_HistorialDiasNoNumerico() throws Exception {
        mockMvc.perform(get("/api/almacenes/ALM02/historial?dias=abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-016: Historial de almacén inexistente (404 Not Found)")
    void testCP_ALM_016_HistorialAlmacenInexistente() throws Exception {
        when(almacenService.obtenerHistorial("ALM99", 5))
                .thenThrow(new ResourceNotFoundException("Almacen", "id", "ALM99"));

        mockMvc.perform(get("/api/almacenes/ALM99/historial"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-ALM-018: Carga manual válida")
    void testCP_ALM_018_CargaManualValida() throws Exception {
        MovimientoInventarioDTO mov = MovimientoInventarioDTO.builder()
                .codigo("CAR-12345678")
                .almacenCodigo("ALM02")
                .cantidad(100)
                .tipo(TipoMovimiento.CARGA)
                .fechaHora(LocalDateTime.now())
                .build();

        when(almacenService.cargarInventario("ALM02", 100)).thenReturn(mov);

        mockMvc.perform(post("/api/almacenes/ALM02/carga?cantidad=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad", is(100)))
                .andExpect(jsonPath("$.almacenCodigo", is("ALM02")));
    }

    @Test
    @DisplayName("CP-ALM-020: Carga que excede el tope de 1,000 u (400 Bad Request)")
    void testCP_ALM_020_CargaExcedeCapacidad() throws Exception {
        when(almacenService.cargarInventario("ALM02", 601))
                .thenThrow(new IllegalArgumentException("Operación rechazada: El stock resultante excede la capacidad máxima de 1000 unidades."));

        mockMvc.perform(post("/api/almacenes/ALM02/carga?cantidad=601"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("CP-ALM-022: Carga con cantidad = 0 (400 Bad Request)")
    void testCP_ALM_022_CargaCantidadCero() throws Exception {
        when(almacenService.cargarInventario("ALM02", 0))
                .thenThrow(new IllegalArgumentException("La cantidad a cargar debe ser un número entero mayor a 0."));

        mockMvc.perform(post("/api/almacenes/ALM02/carga?cantidad=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-023: Carga con cantidad negativa (400 Bad Request)")
    void testCP_ALM_023_CargaCantidadNegativa() throws Exception {
        when(almacenService.cargarInventario("ALM02", -50))
                .thenThrow(new IllegalArgumentException("La cantidad a cargar debe ser un número entero mayor a 0."));

        mockMvc.perform(post("/api/almacenes/ALM02/carga?cantidad=-50"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-024: Carga con cantidad no numérica (400 Bad Request)")
    void testCP_ALM_024_CargaCantidadNoNumerica() throws Exception {
        mockMvc.perform(post("/api/almacenes/ALM02/carga?cantidad=abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-025: Carga con cantidad ausente (400 Bad Request)")
    void testCP_ALM_025_CargaCantidadAusente() throws Exception {
        mockMvc.perform(post("/api/almacenes/ALM02/carga"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-026: Carga a almacén inexistente (404 Not Found)")
    void testCP_ALM_026_CargaAlmacenInexistente() throws Exception {
        when(almacenService.cargarInventario("ALM99", 10))
                .thenThrow(new ResourceNotFoundException("Almacen", "id", "ALM99"));

        mockMvc.perform(post("/api/almacenes/ALM99/carga?cantidad=10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-ALM-028: Carga manual al almacén central rechazada (400 Bad Request)")
    void testCP_ALM_028_CargaManualAlmacenCentralRechazada() throws Exception {
        when(almacenService.cargarInventario("ALM01", 100))
                .thenThrow(new IllegalArgumentException("El almacén central tiene inventario ilimitado y no admite carga manual."));

        mockMvc.perform(post("/api/almacenes/ALM01/carga?cantidad=100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ALM-029: Recarga diaria sin instante")
    void testCP_ALM_029_RecargaDiariaSinInstante() throws Exception {
        doNothing().when(almacenService).ejecutarRecargaDiaria(null);

        mockMvc.perform(post("/api/almacenes/recarga-diaria"))
                .andExpect(status().isOk());

        verify(almacenService, times(1)).ejecutarRecargaDiaria(null);
    }

    @Test
    @DisplayName("CP-ALM-030: Recarga diaria con instante válido ISO-8601")
    void testCP_ALM_030_RecargaDiariaConInstanteValido() throws Exception {
        doNothing().when(almacenService).ejecutarRecargaDiaria(any(LocalDateTime.class));

        mockMvc.perform(post("/api/almacenes/recarga-diaria?instante=2026-10-12T23:59:59"))
                .andExpect(status().isOk());

        verify(almacenService, times(1)).ejecutarRecargaDiaria(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-ALM-031: Recarga con instante inválido (400 Bad Request)")
    void testCP_ALM_031_RecargaConInstanteInvalido() throws Exception {
        mockMvc.perform(post("/api/almacenes/recarga-diaria?instante=12/10/2026"))
                .andExpect(status().isBadRequest());
    }
}
