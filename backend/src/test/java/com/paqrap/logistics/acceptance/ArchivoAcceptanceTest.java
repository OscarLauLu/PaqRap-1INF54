package com.paqrap.logistics.acceptance;

import com.paqrap.logistics.common.controller.ArchivoController;
import com.paqrap.logistics.common.exception.GlobalExceptionHandler;
import com.paqrap.logistics.common.service.ArchivoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de aceptación para el módulo de Carga Auxiliar de Archivos (/api/archivos).
 * Trazabilidad: RF-64, CU-3.14.
 * Casos: CP-ARC-001 a CP-ARC-011.
 */
@ExtendWith(MockitoExtension.class)
public class ArchivoAcceptanceTest {

    private MockMvc mockMvc;

    @Mock
    private ArchivoStorageService archivoStorageService;

    @InjectMocks
    private ArchivoController archivoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(archivoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CP-ARC-001: Subir archivo de pedidos (200 OK con rutaArchivo)")
    void testCP_ARC_001_SubirArchivoPedidos() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "ventas202610.txt",
                "text/plain",
                "01d08h00m,20,10,CLI-001,5,4".getBytes()
        );

        when(archivoStorageService.guardar(any(), eq("pedidos")))
                .thenReturn("/tmp/paqrap_uploads/pedidos/12345-ventas202610.txt");

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file)
                        .param("tipo", "pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaArchivo", containsString("ventas202610.txt")));
    }

    @Test
    @DisplayName("CP-ARC-002: Subir archivo de bloqueos (200 OK con rutaArchivo)")
    void testCP_ARC_002_SubirArchivoBloqueos() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "202610.bloqueadas",
                "text/plain",
                "01d08h00m-01d12h00m:10,5,10,15".getBytes()
        );

        when(archivoStorageService.guardar(any(), eq("bloqueos")))
                .thenReturn("/tmp/paqrap_uploads/bloqueos/12345-202610.bloqueadas");

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file)
                        .param("tipo", "bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rutaArchivo", containsString("202610.bloqueadas")));
    }

    @Test
    @DisplayName("CP-ARC-005: Tipo fuera de catálogo (400 Bad Request)")
    void testCP_ARC_005_TipoFueraDeCatalogo() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "averias.txt",
                "text/plain",
                "contenido".getBytes()
        );

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file)
                        .param("tipo", "averias"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ARC-006: Tipo ausente (400 Bad Request)")
    void testCP_ARC_006_TipoAusente() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "archivo.txt",
                "text/plain",
                "contenido".getBytes()
        );

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ARC-007: Sin archivo (400 Bad Request)")
    void testCP_ARC_007_SinArchivo() throws Exception {
        mockMvc.perform(multipart("/api/archivos/subir")
                        .param("tipo", "pedidos"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-ARC-008: Archivo vacío (400 Bad Request)")
    void testCP_ARC_008_ArchivoVacio() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "vacio.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/archivos/subir")
                        .file(file)
                        .param("tipo", "pedidos"))
                .andExpect(status().isBadRequest());
    }
}
