package com.paqrap.logistics.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paqrap.logistics.common.exception.BusinessException;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.exception.ResourceNotFoundException;
import com.paqrap.logistics.pedidos.controller.PedidoController;
import com.paqrap.logistics.pedidos.dto.ActualizarPedidoDTO;
import com.paqrap.logistics.pedidos.dto.CrearPedidoDTO;
import com.paqrap.logistics.pedidos.dto.PedidoDTO;
import com.paqrap.logistics.pedidos.model.CriticidadSemaforo;
import com.paqrap.logistics.pedidos.model.EstadoPedido;
import com.paqrap.logistics.pedidos.model.TipoEntrega;
import com.paqrap.logistics.pedidos.service.PedidoService;
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
 * Pruebas de aceptación para el módulo de Gestión de Pedidos (/api/pedidos).
 * Trazabilidad: RF-26 a RF-38, RF-04, CU-3.1 a CU-3.4.
 * Casos: CP-PED-001 a CP-PED-074.
 */
@ExtendWith(MockitoExtension.class)
public class PedidoAcceptanceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController pedidoController;

    private PedidoDTO pedidoMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pedidoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        LocalDateTime ahora = LocalDateTime.of(2026, 10, 12, 10, 0, 0);

        pedidoMock = PedidoDTO.builder()
                .id(1L)
                .codigo("PED-001")
                .idCliente("CLI-001")
                .nombreCliente("Comercial Andina SAC")
                .destino(new Ubicacion(35, 25))
                .cantidadUnidades(10)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .estado(EstadoPedido.REGISTRADO)
                .fechaHoraRegistro(ahora)
                .plazoLimiteEntrega(ahora.plusHours(36))
                .holguraHoras(36.0)
                .criticidad(CriticidadSemaforo.VERDE)
                .build();
    }

    @Test
    @DisplayName("CP-PED-001: Registro típico REGULAR_36H (201 Created)")
    void testCP_PED_001_RegistroTipicoRegular36H() throws Exception {
        CrearPedidoDTO dto = CrearPedidoDTO.builder()
                .idCliente("CLI-001")
                .nombreCliente("Comercial Andina SAC")
                .destinoX(35)
                .destinoY(25)
                .cantidadUnidades(10)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .build();

        when(pedidoService.registrarPedido(any(CrearPedidoDTO.class))).thenReturn(pedidoMock);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("PED-001")))
                .andExpect(jsonPath("$.estado", is("REGISTRADO")))
                .andExpect(jsonPath("$.holguraHoras", greaterThan(0.0)));
    }

    @Test
    @DisplayName("CP-PED-002: Registro PRIORIZADA_4H")
    void testCP_PED_002_RegistroPriorizada4H() throws Exception {
        CrearPedidoDTO dto = CrearPedidoDTO.builder()
                .idCliente("CLI-001")
                .nombreCliente("Comercial Andina SAC")
                .destinoX(35)
                .destinoY(25)
                .cantidadUnidades(10)
                .tipoEntrega(TipoEntrega.PRIORIZADA_4H)
                .build();

        PedidoDTO pedido4H = PedidoDTO.builder()
                .id(2L)
                .codigo("PED-002")
                .tipoEntrega(TipoEntrega.PRIORIZADA_4H)
                .plazoLimiteEntrega(LocalDateTime.of(2026, 10, 12, 14, 0))
                .build();

        when(pedidoService.registrarPedido(any(CrearPedidoDTO.class))).thenReturn(pedido4H);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", is("PED-002")));
    }

    @Test
    @DisplayName("CP-PED-006: Destino en límite inferior (0,0), cantidad mínima 1")
    void testCP_PED_006_DestinoLimiteInferior() throws Exception {
        CrearPedidoDTO dto = CrearPedidoDTO.builder()
                .idCliente("CLI-001")
                .nombreCliente("Cliente Límite")
                .destinoX(0)
                .destinoY(0)
                .cantidadUnidades(1)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .build();

        when(pedidoService.registrarPedido(any(CrearPedidoDTO.class))).thenReturn(pedidoMock);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CP-PED-007: Destino en límite superior (70,50)")
    void testCP_PED_007_DestinoLimiteSuperior() throws Exception {
        CrearPedidoDTO dto = CrearPedidoDTO.builder()
                .idCliente("CLI-001")
                .nombreCliente("Cliente Límite")
                .destinoX(70)
                .destinoY(50)
                .cantidadUnidades(10)
                .tipoEntrega(TipoEntrega.REGULAR_36H)
                .build();

        when(pedidoService.registrarPedido(any(CrearPedidoDTO.class))).thenReturn(pedidoMock);

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CP-PED-010: idCliente vacío (400 Bad Request)")
    void testCP_PED_010_IdClienteVacio() throws Exception {
        String body = """
                {
                  "idCliente": "",
                  "nombreCliente": "Comercial Andina",
                  "destinoX": 35,
                  "destinoY": 25,
                  "cantidadUnidades": 10,
                  "tipoEntrega": "REGULAR_36H"
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-015: destinoX = -1 (400 Bad Request)")
    void testCP_PED_015_DestinoXNegativo() throws Exception {
        String body = """
                {
                  "idCliente": "CLI-001",
                  "nombreCliente": "Comercial Andina",
                  "destinoX": -1,
                  "destinoY": 25,
                  "cantidadUnidades": 10,
                  "tipoEntrega": "REGULAR_36H"
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-016: destinoX = 71 fuera de retícula (400 Bad Request)")
    void testCP_PED_016_DestinoXExcedeRetícula() throws Exception {
        String body = """
                {
                  "idCliente": "CLI-001",
                  "nombreCliente": "Comercial Andina",
                  "destinoX": 71,
                  "destinoY": 25,
                  "cantidadUnidades": 10,
                  "tipoEntrega": "REGULAR_36H"
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-021: cantidadUnidades = 0 (400 Bad Request)")
    void testCP_PED_021_CantidadCero() throws Exception {
        String body = """
                {
                  "idCliente": "CLI-001",
                  "nombreCliente": "Comercial Andina",
                  "destinoX": 35,
                  "destinoY": 25,
                  "cantidadUnidades": 0,
                  "tipoEntrega": "REGULAR_36H"
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-025: tipoEntrega fuera del enum (400 Bad Request)")
    void testCP_PED_025_TipoEntregaInvalido() throws Exception {
        String body = """
                {
                  "idCliente": "CLI-001",
                  "nombreCliente": "Comercial Andina",
                  "destinoX": 35,
                  "destinoY": 25,
                  "cantidadUnidades": 10,
                  "tipoEntrega": "EXPRESS_2H"
                }
                """;

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-027: Body vacío {} (400 Bad Request)")
    void testCP_PED_027_BodyVacio() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-PED-028: Listar sin filtros (200 OK)")
    void testCP_PED_028_ListarSinFiltros() throws Exception {
        when(pedidoService.listarPedidos(null, null, null, null)).thenReturn(List.of(pedidoMock));

        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("PED-001")));
    }

    @Test
    @DisplayName("CP-PED-030: Filtrar por cliente sin pedidos (200 OK lista vacía)")
    void testCP_PED_030_FiltrarClienteSinPedidos() throws Exception {
        when(pedidoService.listarPedidos(eq("CLI-777"), any(), any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/pedidos?idCliente=CLI-777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("CP-PED-041: Detalle de pedido existente (200 OK)")
    void testCP_PED_041_DetallePedidoExistente() throws Exception {
        when(pedidoService.obtenerPedido(1L)).thenReturn(pedidoMock);

        mockMvc.perform(get("/api/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo", is("PED-001")))
                .andExpect(jsonPath("$.cliente", is("Comercial Andina SAC")));
    }

    @Test
    @DisplayName("CP-PED-042: Detalle de pedido inexistente (404 Not Found)")
    void testCP_PED_042_DetallePedidoInexistente() throws Exception {
        when(pedidoService.obtenerPedido(999999L))
                .thenThrow(new ResourceNotFoundException("Pedido", "id", 999999L));

        mockMvc.perform(get("/api/pedidos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-PED-047: Pendientes ordenados por holgura (200 OK)")
    void testCP_PED_047_PendientesOrdenadosPorHolgura() throws Exception {
        PedidoDTO p1 = PedidoDTO.builder().codigo("PED-01").holguraHoras(2.0).build();
        PedidoDTO p2 = PedidoDTO.builder().codigo("PED-02").holguraHoras(5.0).build();
        PedidoDTO p3 = PedidoDTO.builder().codigo("PED-03").holguraHoras(20.0).build();

        when(pedidoService.obtenerPedidosPendientesOrdenadosPorHolgura()).thenReturn(List.of(p1, p2, p3));

        mockMvc.perform(get("/api/pedidos/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].holguraHoras", is(2.0)))
                .andExpect(jsonPath("$[1].holguraHoras", is(5.0)))
                .andExpect(jsonPath("$[2].holguraHoras", is(20.0)));
    }

    @Test
    @DisplayName("CP-PED-051: Modificar cantidad en REGISTRADO (200 OK)")
    void testCP_PED_051_ModificarCantidad() throws Exception {
        ActualizarPedidoDTO dto = ActualizarPedidoDTO.builder().cantidadUnidades(20).build();
        PedidoDTO modificado = PedidoDTO.builder().id(1L).cantidadUnidades(20).build();

        when(pedidoService.modificarPedido(eq(1L), any(ActualizarPedidoDTO.class))).thenReturn(modificado);

        mockMvc.perform(put("/api/pedidos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadUnidades", is(20)));
    }

    @Test
    @DisplayName("CP-PED-056: Modificar pedido PLANIFICADO rechazado (409 Conflict)")
    void testCP_PED_056_ModificarPedidoPlanificadoRechazado() throws Exception {
        ActualizarPedidoDTO dto = ActualizarPedidoDTO.builder().cantidadUnidades(5).build();

        when(pedidoService.modificarPedido(eq(1L), any(ActualizarPedidoDTO.class)))
                .thenThrow(new BusinessException("No se puede modificar un pedido posterior a su asignación a una ruta (PLANIFICADO)"));

        mockMvc.perform(put("/api/pedidos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CP-PED-065: Cancelar pedido REGISTRADO (204 No Content)")
    void testCP_PED_065_CancelarPedidoRegistrado() throws Exception {
        doNothing().when(pedidoService).cancelarPedido(1L);

        mockMvc.perform(delete("/api/pedidos/1"))
                .andExpect(status().isNoContent());

        verify(pedidoService, times(1)).cancelarPedido(1L);
    }

    @Test
    @DisplayName("CP-PED-066: Cancelar pedido PLANIFICADO rechazado (409 Conflict)")
    void testCP_PED_066_CancelarPedidoPlanificadoRechazado() throws Exception {
        doThrow(new BusinessException("No se puede cancelar un pedido posterior a su asignación a una ruta"))
                .when(pedidoService).cancelarPedido(1L);

        mockMvc.perform(delete("/api/pedidos/1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CP-PED-067: Cancelar pedido inexistente (404 Not Found)")
    void testCP_PED_067_CancelarPedidoInexistente() throws Exception {
        doThrow(new ResourceNotFoundException("Pedido", "id", 999999L))
                .when(pedidoService).cancelarPedido(999999L);

        mockMvc.perform(delete("/api/pedidos/999999"))
                .andExpect(status().isNotFound());
    }
}
