package com.paqrap.logistics.flota.service;

import com.paqrap.logistics.flota.model.Mantenimiento;
import com.paqrap.logistics.flota.model.MantenimientoProgramado;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import com.paqrap.logistics.flota.repository.MantenimientoProgramadoRepository;
import com.paqrap.logistics.flota.repository.UnidadTransporteRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plan de mantenimiento preventivo de la flota (FAQ 19, RF-42).
 * <ul>
 *   <li>Archivos bimestrales {@code mant.preventivo.m1.m2}, registros {@code aaaammdd:TTNN}.</li>
 *   <li>Una unidad en mantenimiento no está disponible desde las 00:00 hasta las 23:59 de ese día,
 *       sea auto, moto o bicicleta.</li>
 *   <li>La planificación debe evitar que una unidad siga en ruta cuando empieza su mantenimiento.</li>
 * </ul>
 * El plan es de solo lectura: lo consultan la operación diaria y cada sesión de simulación.
 */
@Slf4j
@Service
public class PlanMantenimientoService {

    /** Carpeta con los planes 2026-2028 generados a partir del plan oficial 09.10. */
    static final String CARPETA_PLANES = "mantenimiento";

    private volatile List<Mantenimiento> plan = Collections.emptyList();

    /** Opcional: nulo cuando el servicio se crea a mano (tests); Spring lo inyecta en la app. */
    @Autowired(required = false)
    private MantenimientoProgramadoRepository repositorio;

    @Autowired(required = false)
    private UnidadTransporteRepository unidadRepositorio;

    /** Lo leído de los archivos; base para sincronizar con la BD. */
    private volatile List<Mantenimiento> deArchivo = Collections.emptyList();

    @PostConstruct
    void cargarAlIniciar() {
        Path carpeta = resolverCarpetaDatos().resolve(CARPETA_PLANES);
        cargarDesdeCarpeta(carpeta);
    }

    /** Carga todos los archivos {@code mant.preventivo.*} de la carpeta; reemplaza el plan anterior. */
    public void cargarDesdeCarpeta(Path carpeta) {
        List<Mantenimiento> nuevo = new ArrayList<>();
        if (Files.isDirectory(carpeta)) {
            try (Stream<Path> archivos = Files.list(carpeta)) {
                archivos.filter(p -> p.getFileName().toString().startsWith("mant.preventivo"))
                        .sorted()
                        .forEach(p -> nuevo.addAll(parsearArchivo(p)));
            } catch (IOException e) {
                log.error("No se pudo leer la carpeta de mantenimiento {}: {}", carpeta, e.getMessage());
            }
        } else {
            log.warn("No existe la carpeta de planes de mantenimiento: {}. No se aplicará mantenimiento.", carpeta);
        }
        this.deArchivo = nuevo;
        this.plan = Collections.unmodifiableList(nuevo);
        log.info("Plan de mantenimiento preventivo cargado de archivos: {} registros.", nuevo.size());
    }

    /**
     * Sincroniza el plan con la tabla {@code mantenimiento_programado}. Corre cuando la app ya
     * arrancó (la flota ya está sembrada, que es lo que exige la FK) y antes de que inicie la
     * operación diaria. Guarda lo de archivo que aún no existe (idempotente) y deja como plan
     * vigente lo que haya en la tabla, de modo que un día de mantenimiento agregado directamente
     * en BD también bloquea la unidad. Sin repositorios (tests) o si la tabla no está disponible,
     * el plan sigue siendo el leído de archivo.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void sincronizarConBd() {
        if (repositorio == null || unidadRepositorio == null) {
            return;
        }
        try {
            Set<String> flota = unidadRepositorio.findAll().stream()
                    .map(UnidadTransporte::getCodigo)
                    .collect(Collectors.toSet());
            Set<String> existentes = repositorio.findAll().stream()
                    .map(MantenimientoProgramado::getCodigo)
                    .collect(Collectors.toSet());
            List<MantenimientoProgramado> faltantes = new ArrayList<>();
            int sinUnidad = 0;
            for (Mantenimiento m : deArchivo) {
                String codigo = codigoDe(m);
                if (!flota.contains(m.getCodigoVehiculo())) {
                    sinUnidad++;
                } else if (existentes.add(codigo)) {
                    faltantes.add(MantenimientoProgramado.builder()
                            .codigo(codigo)
                            .unidad(unidadRepositorio.getReferenceById(m.getCodigoVehiculo()))
                            .fecha(m.getFechaHoraInicio().toLocalDate())
                            .horaInicio(m.getFechaHoraInicio().toLocalTime())
                            .horaFin(m.getFechaHoraFin().toLocalTime())
                            .build());
                }
            }
            if (sinUnidad > 0) {
                log.warn("{} registros de mantenimiento se omitieron: su unidad no existe en la flota.", sinUnidad);
            }
            if (!faltantes.isEmpty()) {
                repositorio.saveAll(faltantes);
                log.info("Mantenimientos nuevos guardados en BD: {}", faltantes.size());
            }
            this.plan = Collections.unmodifiableList(repositorio.findAllConUnidad().stream()
                    .map(r -> Mantenimiento.builder()
                            .codigoVehiculo(r.getUnidad().getCodigo())
                            .fechaHoraInicio(r.getFecha().atTime(r.getHoraInicio()))
                            .fechaHoraFin(r.getFecha().atTime(r.getHoraFin()))
                            .build())
                    .collect(Collectors.toList()));
            log.info("Plan de mantenimiento vigente desde BD: {} registros.", this.plan.size());
        } catch (RuntimeException e) {
            log.warn("No se pudo usar la tabla mantenimiento_programado; se usa solo el plan de archivos: {}", e.getMessage());
        }
    }

    private static String codigoDe(Mantenimiento m) {
        return "MNT-" + m.getFechaHoraInicio().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + m.getCodigoVehiculo();
    }

    /**
     * Lee un archivo con registros {@code aaaammdd:TTNN}. Cada registro bloquea la unidad
     * desde las 00:00:00 hasta las 23:59:59 de ese día (FAQ 19, nota 1).
     */
    public static List<Mantenimiento> parsearArchivo(Path archivo) {
        List<Mantenimiento> registros = new ArrayList<>();
        try {
            int numLinea = 0;
            for (String linea : Files.readAllLines(archivo)) {
                numLinea++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                String[] partes = linea.split(":");
                if (partes.length != 2 || partes[0].trim().length() != 8) {
                    log.warn("{} línea {}: registro inválido '{}'", archivo.getFileName(), numLinea, linea);
                    continue;
                }
                try {
                    String f = partes[0].trim();
                    LocalDate fecha = LocalDate.of(
                            Integer.parseInt(f.substring(0, 4)),
                            Integer.parseInt(f.substring(4, 6)),
                            Integer.parseInt(f.substring(6, 8)));
                    registros.add(Mantenimiento.builder()
                            .codigoVehiculo(partes[1].trim())
                            .fechaHoraInicio(fecha.atStartOfDay())
                            .fechaHoraFin(fecha.atTime(23, 59, 59))
                            .build());
                } catch (RuntimeException e) {
                    log.warn("{} línea {}: fecha inválida '{}'", archivo.getFileName(), numLinea, linea);
                }
            }
        } catch (IOException e) {
            log.error("No se pudo leer el archivo de mantenimiento {}: {}", archivo, e.getMessage());
        }
        return registros;
    }

    /** true si la unidad está en su día de mantenimiento en ese instante. */
    public boolean estaEnMantenimiento(String codigoUnidad, LocalDateTime instante) {
        if (codigoUnidad == null || instante == null) return false;
        return plan.stream().anyMatch(m -> codigoUnidad.equals(m.getCodigoVehiculo()) && m.estaActivoEn(instante));
    }

    /**
     * Primer mantenimiento de la unidad que empieza dentro de (desde, hasta]. Sirve para rechazar
     * rutas que harían que la unidad siga en ruta a las 00:00 de su día de mantenimiento.
     */
    public Optional<Mantenimiento> mantenimientoQueIniciaEntre(String codigoUnidad, LocalDateTime desde, LocalDateTime hasta) {
        if (codigoUnidad == null || desde == null || hasta == null) return Optional.empty();
        return plan.stream()
                .filter(m -> codigoUnidad.equals(m.getCodigoVehiculo()))
                .filter(m -> m.getFechaHoraInicio().isAfter(desde) && !m.getFechaHoraInicio().isAfter(hasta))
                .findFirst();
    }

    public List<Mantenimiento> getPlan() {
        return plan;
    }

    /** Misma convención que el resto del backend: "datos" en Docker, "../datos" desde backend/. */
    static Path resolverCarpetaDatos() {
        Path datos = Paths.get("datos");
        return Files.isDirectory(datos) ? datos : Paths.get("..", "datos");
    }
}
