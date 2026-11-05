package com.paqrap.logistics.simulacion.model;

import java.io.File;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Ubica los archivos mensuales oficiales del caso dentro de la carpeta {@code datos}:
 * <ul>
 *   <li>Pedidos (FAQ 8): {@code ventas.v20260909/ventas.aaaamm.txt}, enero 2026 a diciembre 2028.</li>
 *   <li>Bloqueos (FAQ 7): {@code bloqueos/bloqueo.aamm.txt}, mismos meses.</li>
 * </ul>
 * Reemplaza las rutas fijas a septiembre 2026: el archivo se elige según el mes simulado.
 */
public final class DirectorioDatos {

    static final String CARPETA_VENTAS = "ventas.v20260909";
    static final String CARPETA_BLOQUEOS = "bloqueos";

    private DirectorioDatos() {
    }

    /** "datos" cuando corre en Docker (/app/datos) y "../datos" cuando corre desde backend/. */
    public static File raiz() {
        File datos = new File("datos");
        return datos.isDirectory() ? datos : new File("..", "datos");
    }

    public static Optional<String> archivoVentas(YearMonth mes) {
        return archivoVentas(raiz(), mes);
    }

    public static Optional<String> archivoBloqueos(YearMonth mes) {
        return archivoBloqueos(raiz(), mes);
    }

    static Optional<String> archivoVentas(File raiz, YearMonth mes) {
        String nombre = String.format("ventas.%04d%02d.txt", mes.getYear(), mes.getMonthValue());
        return existente(new File(new File(raiz, CARPETA_VENTAS), nombre));
    }

    static Optional<String> archivoBloqueos(File raiz, YearMonth mes) {
        String nombre = String.format("bloqueo.%02d%02d.txt", mes.getYear() % 100, mes.getMonthValue());
        return existente(new File(new File(raiz, CARPETA_BLOQUEOS), nombre));
    }

    private static Optional<String> existente(File archivo) {
        return archivo.isFile() ? Optional.of(archivo.getAbsolutePath()) : Optional.empty();
    }
}
