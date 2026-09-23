package com.paqrap.logistics.common.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Guarda en disco los archivos planos subidos desde el frontend (pedidos, bloqueos) para que
 * puedan procesarse con la misma lógica de {@code CargadorArchivos} que ya usan los flujos de
 * carga por ruta de archivo (RF-11, RF-64), en vez de duplicar el parseo en un endpoint aparte.
 */
@Service
public class ArchivoStorageService {

    private static final Path CARPETA_BASE = Paths.get("datos", "uploads");

    public String guardar(MultipartFile archivo, String subcarpeta) throws IOException {
        Path dir = CARPETA_BASE.resolve(subcarpeta);
        Files.createDirectories(dir);
        String nombreLimpio = System.currentTimeMillis() + "-" + archivo.getOriginalFilename();
        Path destino = dir.resolve(nombreLimpio);
        archivo.transferTo(destino);
        return destino.toString();
    }
}
