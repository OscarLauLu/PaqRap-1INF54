package com.paqrap.logistics.redvial.service;

import com.paqrap.logistics.redvial.dto.CrearBloqueoManualDTO;
import com.paqrap.logistics.redvial.model.Bloqueo;
import com.paqrap.logistics.redvial.model.Nodo;
import com.paqrap.logistics.redvial.model.RedVial;
import com.paqrap.logistics.redvial.model.Tramo;
import com.paqrap.logistics.redvial.repository.BloqueoRepository;
import com.paqrap.logistics.simulacion.model.CargadorArchivos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedVialService {

    private final RedVial redVial;
    private final BloqueoRepository bloqueoRepository;
    private final CargadorArchivos cargadorArchivos;

    public Nodo obtenerNodo(int x, int y) {
        return redVial.obtenerNodo(x, y);
    }

    public double calcularDistancia(int x1, int y1, int x2, int y2) {
        Nodo o = redVial.obtenerNodo(x1, y1);
        Nodo d = redVial.obtenerNodo(x2, y2);
        return redVial.calcularDistancia(o, d);
    }

    public List<Tramo> calcularRutaMinima(int x1, int y1, int x2, int y2, LocalDateTime instante) {
        Nodo o = redVial.obtenerNodo(x1, y1);
        Nodo d = redVial.obtenerNodo(x2, y2);
        return redVial.calcularRutaMinima(o, d, instante != null ? instante : LocalDateTime.now());
    }

    public List<Bloqueo> listarBloqueosActivos() {
        return bloqueoRepository.findByActivoTrue();
    }

    public List<Bloqueo> listarTodosBloqueos() {
        return bloqueoRepository.findAll();
    }

    /**
     * Carga y activa el archivo mensual de bloqueos (aaaamm.bloqueadas) (RF-11).
     */
    @Transactional
    public List<Bloqueo> cargarArchivoBloqueos(String rutaArchivo) {
        List<Bloqueo> cargados = cargadorArchivos.cargarBloqueos(rutaArchivo);
        for (Bloqueo b : cargados) {
            bloqueoRepository.save(b);
            if (b.estaVigente(LocalDateTime.now())) {
                b.activar();
                redVial.aplicarBloqueo(b);
                bloqueoRepository.save(b);
            }
        }
        log.info("Persistidos y procesados {} bloqueos viales desde {}", cargados.size(), rutaArchivo);
        return cargados;
    }

    /**
     * Registra manualmente un bloqueo individual (a diferencia de la carga masiva por
     * archivo mensual). Distinto flujo en el frontend: "Registro manual" vs "Subir archivo".
     */
    @Transactional
    public Bloqueo registrarBloqueoManual(CrearBloqueoManualDTO dto) {
        Bloqueo bloqueo = Bloqueo.builder()
                .codigo("BLOQ-MAN-" + UUID.randomUUID().toString().substring(0, 8))
                .fechaHoraInicio(dto.getFechaHoraInicio())
                .fechaHoraFin(dto.getFechaHoraFin())
                .coordenadasNodos(dto.getCoordenadasNodos())
                .archivoOrigen("MANUAL")
                .activo(false)
                .build();

        bloqueoRepository.save(bloqueo);
        if (bloqueo.estaVigente(LocalDateTime.now())) {
            bloqueo.activar();
            redVial.aplicarBloqueo(bloqueo);
            bloqueoRepository.save(bloqueo);
        }
        log.info("Bloqueo manual registrado: {}", bloqueo.getCodigo());
        return bloqueo;
    }
}
