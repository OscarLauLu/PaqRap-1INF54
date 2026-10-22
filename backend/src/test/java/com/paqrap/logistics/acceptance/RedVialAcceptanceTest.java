package com.paqrap.logistics.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.service.ArchivoStorageService;
import com.paqrap.logistics.redvial.controller.RedVialController;
import com.paqrap.logistics.redvial.dto.CrearBloqueoManualDTO;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.Tramo;
import com.paqrap.logistics.redvial.service.RedVialService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación para el módulo de Red Vial y Geografía Urbana (/api/redvial).
 * Trazabilidad: RF-01, RF-10, RF-11, RF-12, CU-3.5 y CU-3.6.
 * Casos: CP-RVL-001 a CP-RVL-060.
 */
@ExtendWith(MockitoExtension.class)
public class RedVialAcceptanceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RedVialService redVialService;

    @Mock
    private ArchivoStorageService archivoStorageService;

    @InjectMocks
    private RedVialController redVialController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(redVialController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("CP-RVL-001: Nodo interior (200 OK)")
    void testCP_RVL_001_NodoInterior() throws Exception {
        Nodo nodoMock = new Nodo(35, 25);
        when(redVialService.obtenerNodo(35, 25)).thenReturn(nodoMock);

        mockMvc.perform(get("/api/redvial/nodo?x=35&y=25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x", is(35)))
                .andExpect(jsonPath("$.y", is(25)));
    }

    @Test
    @DisplayName("CP-RVL-002: Nodo esquina (0,0) (200 OK)")
    void testCP_RVL_002_NodoEsquinaOrigen() throws Exception {
        Nodo nodoMock = new Nodo(0, 0);
        when(redVialService.obtenerNodo(0, 0)).thenReturn(nodoMock);

        mockMvc.perform(get("/api/redvial/nodo?x=0&y=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x", is(0)))
                .andExpect(jsonPath("$.y", is(0)));
    }

    @Test
    @DisplayName("CP-RVL-003: Nodo esquina (70,50) (200 OK)")
    void testCP_RVL_003_NodoEsquinaMax() throws Exception {
        Nodo nodoMock = new Nodo(70, 50);
        when(redVialService.obtenerNodo(70, 50)).thenReturn(nodoMock);

        mockMvc.perform(get("/api/redvial/nodo?x=70&y=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x", is(70)))
                .andExpect(jsonPath("$.y", is(50)));
    }

    @Test
    @DisplayName("CP-RVL-006: x = -1 (400 Bad Request)")
    void testCP_RVL_006_XNegativo() throws Exception {
        when(redVialService.obtenerNodo(-1, 25))
                .thenThrow(new IllegalArgumentException("Coordenadas fuera del rango."));

        mockMvc.perform(get("/api/redvial/nodo?x=-1&y=25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-RVL-007: x = 71 (400 Bad Request)")
    void testCP_RVL_007_XExcedeRango() throws Exception {
        when(redVialService.obtenerNodo(71, 25))
                .thenThrow(new IllegalArgumentException("Coordenadas fuera del rango."));

        mockMvc.perform(get("/api/redvial/nodo?x=71&y=25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-RVL-008: x no entero (400 Bad Request)")
    void testCP_RVL_008_XNoEntero() throws Exception {
        mockMvc.perform(get("/api/redvial/nodo?x=abc&y=25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-RVL-011: y ausente (400 Bad Request)")
    void testCP_RVL_011_YAusente() throws Exception {
        mockMvc.perform(get("/api/redvial/nodo?x=35"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-RVL-012: Distancia Manhattan entre puntos distintos (200 OK)")
    void testCP_RVL_012_DistanciaManhattan() throws Exception {
        when(redVialService.calcularDistancia(10, 5, 20, 15)).thenReturn(20.0);

        mockMvc.perform(get("/api/redvial/distancia?x1=10&y1=5&x2=20&y2=15"))
                .andExpect(status().isOk())
                .andExpect(content().string("20.0"));
    }

    @Test
    @DisplayName("CP-RVL-014: Distancia entre el mismo punto = 0.0 (200 OK)")
    void testCP_RVL_014_DistanciaMismoPunto() throws Exception {
        when(redVialService.calcularDistancia(3, 4, 3, 4)).thenReturn(0.0);

        mockMvc.perform(get("/api/redvial/distancia?x1=3&y1=4&x2=3&y2=4"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }

    @Test
    @DisplayName("CP-RVL-016: Distancia entre extremos opuestos de la retícula = 120.0 km")
    void testCP_RVL_016_DistanciaExtremosOpuestos() throws Exception {
        when(redVialService.calcularDistancia(0, 0, 70, 50)).thenReturn(120.0);

        mockMvc.perform(get("/api/redvial/distancia?x1=0&y1=0&x2=70&y2=50"))
                .andExpect(status().isOk())
                .andExpect(content().string("120.0"));
    }

    @Test
    @DisplayName("CP-RVL-020: Ruta mínima sin bloqueos (200 OK)")
    void testCP_RVL_020_RutaMinimaSinBloqueos() throws Exception {
        Tramo t1 = new Tramo(new Nodo(0, 0), new Nodo(1, 0));
        when(redVialService.calcularRutaMinima(eq(0), eq(0), eq(10), eq(10), any()))
                .thenReturn(List.of(t1));

        mockMvc.perform(get("/api/redvial/ruta-minima?x1=0&y1=0&x2=10&y2=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("CP-RVL-031: Listar bloqueos sin parámetro (200 OK)")
    void testCP_RVL_031_ListarBloqueosSinParametro() throws Exception {
        Bloqueo b = Bloqueo.builder()
                .codigo("BLOQ-01")
                .fechaHoraInicio(LocalDateTime.of(2026, 10, 12, 8, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 10, 12, 12, 0))
                .coordenadasNodos("10,5,10,15")
                .activo(true)
                .build();

        when(redVialService.listarTodosBloqueos()).thenReturn(List.of(b));

        mockMvc.perform(get("/api/redvial/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("BLOQ-01")));
    }

    @Test
    @DisplayName("CP-RVL-032: Listar solo bloqueos activos (200 OK)")
    void testCP_RVL_032_ListarSoloActivos() throws Exception {
        when(redVialService.listarBloqueosActivos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/redvial/bloqueos?soloActivos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("CP-RVL-036: Cargar archivo mensual válido (200 OK)")
    void testCP_RVL_036_CargarArchivoMensualValido() throws Exception {
        Bloqueo b = Bloqueo.builder().codigo("BLOQ-01").build();
        when(redVialService.cargarArchivoBloqueos("/datos/bloqueo.2610.txt")).thenReturn(List.of(b));

        mockMvc.perform(post("/api/redvial/bloqueos/cargar?rutaArchivo=/datos/bloqueo.2610.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("CP-RVL-043: Cargar archivo sin rutaArchivo (400 Bad Request)")
    void testCP_RVL_043_CargarArchivoSinRutaArchivo() throws Exception {
        mockMvc.perform(post("/api/redvial/bloqueos/cargar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-RVL-050: Crear bloqueo manual de 1 tramo (201 Created)")
    void testCP_RVL_050_CrearBloqueoManualUnTramo() throws Exception {
        CrearBloqueoManualDTO dto = CrearBloqueoManualDTO.builder()
                .fechaHoraInicio(LocalDateTime.of(2026, 10, 12, 8, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 10, 12, 12, 0))
                .coordenadasNodos("10,5,10,15")
                .build();

        Bloqueo b = Bloqueo.builder()
                .id(1L)
                .codigo("BLOQ-MAN-01")
                .fechaHoraInicio(dto.getFechaHoraInicio())
                .fechaHoraFin(dto.getFechaHoraFin())
                .coordenadasNodos(dto.getCoordenadasNodos())
                .build();

        when(redVialService.registrarBloqueoManual(any(CrearBloqueoManualDTO.class))).thenReturn(b);

        mockMvc.perform(post("/api/redvial/bloqueos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("BLOQ-MAN-01")));
    }
}
