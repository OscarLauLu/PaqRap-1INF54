package com.paqrap.logistics.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.flota.controller.FlotaController;
import com.paqrap.logistics.flota.dto.ActualizarTipoVehiculoDTO;
import com.paqrap.logistics.flota.dto.RegistrarAveriaDTO;
import com.paqrap.logistics.flota.dto.UnidadTransporteDTO;
import com.paqrap.logistics.flota.model.Averia;
import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.TipoAveria;
import com.paqrap.logistics.flota.model.TipoVehiculo;
import com.paqrap.logistics.flota.service.FlotaService;
import com.paqrap.logistics.redvial.model.Ubicacion;
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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de aceptación para el módulo de Flota y Conductores (/api/flota).
 * Trazabilidad: RF-39 a RF-50, CU-3.7, CU-3.10, CU-3.11 y CU-3.12.
 * Casos: CP-FLO-001 a CP-FLO-080.
 */
@ExtendWith(MockitoExtension.class)
public class FlotaAcceptanceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FlotaService flotaService;

    @InjectMocks
    private FlotaController flotaController;

    private UnidadTransporteDTO autoMock;
    private UnidadTransporteDTO motoMock;
    private UnidadTransporteDTO biciMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flotaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        autoMock = UnidadTransporteDTO.builder()
                .codigo("TA01")
                .tipo("Auto")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .cargaActual(0)
                .capacidadMaxima(24)
                .porcentajeCapacidad(0.0)
                .ubicacionActual(new Ubicacion(27, 14))
                .activo(true)
                .build();

        motoMock = UnidadTransporteDTO.builder()
                .codigo("TM01")
                .tipo("Moto")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .cargaActual(0)
                .capacidadMaxima(8)
                .porcentajeCapacidad(0.0)
                .ubicacionActual(new Ubicacion(27, 14))
                .activo(true)
                .build();

        biciMock = UnidadTransporteDTO.builder()
                .codigo("TB01")
                .tipo("Bicicleta")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .cargaActual(0)
                .capacidadMaxima(4)
                .porcentajeCapacidad(0.0)
                .ubicacionActual(new Ubicacion(27, 14))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("CP-FLO-001: Listar unidades sin filtros (200 OK)")
    void testCP_FLO_001_ListarUnidadesSinFiltros() throws Exception {
        when(flotaService.listarUnidades(null, null)).thenReturn(List.of(autoMock, motoMock, biciMock));

        mockMvc.perform(get("/api/flota/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].codigo", is("TA01")))
                .andExpect(jsonPath("$[1].codigo", is("TM01")))
                .andExpect(jsonPath("$[2].codigo", is("TB01")));
    }

    @Test
    @DisplayName("CP-FLO-002: Filtrar por tipo Auto (200 OK)")
    void testCP_FLO_002_FiltrarPorTipoAuto() throws Exception {
        when(flotaService.listarUnidades("Auto", null)).thenReturn(List.of(autoMock));

        mockMvc.perform(get("/api/flota/unidades?tipo=Auto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo", is("Auto")));
    }

    @Test
    @DisplayName("CP-FLO-006: Filtrar por estado DISPONIBLE (200 OK)")
    void testCP_FLO_006_FiltrarPorEstadoDisponible() throws Exception {
        when(flotaService.listarUnidades(null, EstadoOperativo.DISPONIBLE)).thenReturn(List.of(autoMock));

        mockMvc.perform(get("/api/flota/unidades?estado=DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoOperativo", is("DISPONIBLE")));
    }

    @Test
    @DisplayName("CP-FLO-009: Estado fuera de catálogo (400 Bad Request)")
    void testCP_FLO_009_EstadoFueraDeCatalogo() throws Exception {
        mockMvc.perform(get("/api/flota/unidades?estado=VOLANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-FLO-011: Unidades disponibles sin tipo (200 OK)")
    void testCP_FLO_011_UnidadesDisponiblesSinTipo() throws Exception {
        when(flotaService.listarUnidadesDisponibles(null)).thenReturn(List.of(autoMock, motoMock, biciMock));

        mockMvc.perform(get("/api/flota/unidades/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("CP-FLO-014: Detalle de unidad Auto (200 OK)")
    void testCP_FLO_014_DetalleUnidadAuto() throws Exception {
        when(flotaService.obtenerUnidad("TA01")).thenReturn(autoMock);

        mockMvc.perform(get("/api/flota/unidades/TA01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("TA01")))
                .andExpect(jsonPath("$.capacidadMaxima", is(24)));
    }

    @Test
    @DisplayName("CP-FLO-017: Detalle de unidad inexistente (404 Not Found)")
    void testCP_FLO_017_UnidadInexistente() throws Exception {
        when(flotaService.obtenerUnidad("TA99"))
                .thenThrow(new ResourceNotFoundException("UnidadTransporte", "codigo", "TA99"));

        mockMvc.perform(get("/api/flota/unidades/TA99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-FLO-019: Cambiar estado a DISPONIBLE (200 OK)")
    void testCP_FLO_019_CambiarEstadoDisponible() throws Exception {
        when(flotaService.cambiarEstadoOperativo("TA01", EstadoOperativo.DISPONIBLE)).thenReturn(autoMock);

        mockMvc.perform(put("/api/flota/unidades/TA01/estado?nuevoEstado=DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo", is("DISPONIBLE")));
    }

    @Test
    @DisplayName("CP-FLO-022: Estado fuera de catálogo en cambio de estado (400 Bad Request)")
    void testCP_FLO_022_EstadoFueraDeCatalogoEnCambio() throws Exception {
        mockMvc.perform(put("/api/flota/unidades/TA01/estado?nuevoEstado=VOLANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-FLO-025: Avería TIPO_1 en ruta (200 OK)")
    void testCP_FLO_025_AveriaTipo1() throws Exception {
        RegistrarAveriaDTO dto = RegistrarAveriaDTO.builder()
                .tipo(TipoAveria.TIPO_1)
                .ubicacionX(20)
                .ubicacionY(10)
                .origenManual(true)
                .build();

        Averia averiaMock = Averia.builder()
                .id(1L)
                .tipo(TipoAveria.TIPO_1)
                .ubicacionAveria(new Ubicacion(20, 10))
                .origenManual(true)
                .build();

        when(flotaService.registrarAveria(eq("TA01"), any(RegistrarAveriaDTO.class))).thenReturn(averiaMock);

        mockMvc.perform(post("/api/flota/unidades/TA01/averia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("TIPO_1")));
    }

    @Test
    @DisplayName("CP-FLO-031: Tipo de avería fuera del enum (400 Bad Request)")
    void testCP_FLO_031_TipoAveriaFueraDelEnum() throws Exception {
        String body = """
                {
                  "tipo": "TIPO_4",
                  "ubicacionX": 20,
                  "ubicacionY": 10
                }
                """;

        mockMvc.perform(post("/api/flota/unidades/TA01/averia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-FLO-039: Doble avería sobre la misma unidad (409 Conflict)")
    void testCP_FLO_039_DobleAveriaRechazada() throws Exception {
        RegistrarAveriaDTO dto = RegistrarAveriaDTO.builder()
                .tipo(TipoAveria.TIPO_1)
                .ubicacionX(20)
                .ubicacionY(10)
                .build();

        when(flotaService.registrarAveria(eq("TA01"), any(RegistrarAveriaDTO.class)))
                .thenThrow(new BusinessException("La unidad TA01 ya se encuentra averiada."));

        mockMvc.perform(post("/api/flota/unidades/TA01/averia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CP-FLO-041: Actualizar los tres parámetros de Auto (200 OK)")
    void testCP_FLO_041_ActualizarParametrosAuto() throws Exception {
        ActualizarTipoVehiculoDTO dto = ActualizarTipoVehiculoDTO.builder()
                .nuevaVelocidadKmH(50.0)
                .nuevoCostoPorKm(2.5)
                .nuevaCapacidadMaxima(20)
                .build();

        TipoVehiculo tipoActualizado = TipoVehiculo.builder()
                .codigo("TA")
                .nombre("Auto")
                .velocidadPromedioKmH(50.0)
                .costoPorKm(2.5)
                .capacidadMaxima(20)
                .build();

        when(flotaService.actualizarConfiguracionTipoVehiculo(eq("Auto"), any(ActualizarTipoVehiculoDTO.class)))
                .thenReturn(tipoActualizado);

        mockMvc.perform(put("/api/flota/configuracion/Auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.velocidadPromedioKmH", is(50.0)))
                .andExpect(jsonPath("$.costoPorKm", is(2.5)))
                .andExpect(jsonPath("$.capacidadMaxima", is(20)));
    }

    @Test
    @DisplayName("CP-FLO-068: Baja de unidad disponible (204 No Content)")
    void testCP_FLO_068_BajaUnidadDisponible() throws Exception {
        doNothing().when(flotaService).darDeBajaUnidad("TA05");

        mockMvc.perform(delete("/api/flota/unidades/TA05"))
                .andExpect(status().isNoContent());

        verify(flotaService, times(1)).darDeBajaUnidad("TA05");
    }

    @Test
    @DisplayName("CP-FLO-069: Baja de unidad inexistente (404 Not Found)")
    void testCP_FLO_069_BajaUnidadInexistente() throws Exception {
        doThrow(new ResourceNotFoundException("UnidadTransporte", "codigo", "TA99"))
                .when(flotaService).darDeBajaUnidad("TA99");

        mockMvc.perform(delete("/api/flota/unidades/TA99"))
                .andExpect(status().isNotFound());
    }
}
