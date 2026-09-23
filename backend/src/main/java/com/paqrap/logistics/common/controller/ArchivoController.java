package com.paqrap.logistics.common.controller;

import com.paqrap.logistics.common.service.ArchivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Sube un archivo plano (pedidos o bloqueos, RF-64) desde el navegador y lo deja disponible en el
 * servidor, para usarlo luego con {@code ParametrosSimulacion.archivoPedidos/archivoBloqueos} al
 * configurar una corrida (pantalla "Configuración de la simulación"). No crea pedidos ni bloqueos
 * por sí mismo: solo deja el archivo listo, igual que el resto de flujos basados en ruta de archivo.
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
@Tag(name = "Carga de Archivos", description = "Sube archivos planos de pedidos/bloqueos al servidor para usarlos al configurar una simulación (RF-64)")
public class ArchivoController {

    private final ArchivoStorageService archivoStorageService;

    @PostMapping("/subir")
    @Operation(summary = "Subir un archivo (tipo=pedidos|bloqueos) y devolver la ruta donde quedó guardado")
    public ResponseEntity<Map<String, String>> subir(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String tipo) throws IOException {
        String ruta = archivoStorageService.guardar(archivo, tipo);
        return ResponseEntity.ok(Map.of("rutaArchivo", ruta));
    }
}
