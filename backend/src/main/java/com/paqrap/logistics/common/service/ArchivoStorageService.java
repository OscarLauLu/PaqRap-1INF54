package com.paqrap.logistics.common.service;

import org.springframework.beans.factory.annotation.Value;
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

    private final Path carpetaBase;

    public ArchivoStorageService(@Value("${logistics.uploads.dir:}") String carpetaConfigurada) {
        this.carpetaBase = (carpetaConfigurada == null || carpetaConfigurada.isBlank())
                ? Paths.get(System.getProperty("java.io.tmpdir"), "paqrap_uploads")
                : Paths.get(carpetaConfigurada);
    }

    public String guardar(MultipartFile archivo, String subcarpeta) throws IOException {
        Path dir = carpetaBase.resolve(subcarpeta).toAbsolutePath();
        Files.createDirectories(dir);
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
            nombreOriginal = "archivo.txt";
        }
        nombreOriginal = Paths.get(nombreOriginal).getFileName().toString();
        String nombreLimpio = System.currentTimeMillis() + "-" + nombreOriginal;
        Path destino = dir.resolve(nombreLimpio);
        archivo.transferTo(destino.toFile());
        return destino.toString();
    }
}
