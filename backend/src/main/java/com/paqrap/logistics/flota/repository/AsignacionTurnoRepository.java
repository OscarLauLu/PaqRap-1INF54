package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.AsignacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, Long> {
    List<AsignacionTurno> findByFecha(LocalDate fecha);
    // Antes findByUnidadId(Long)/findByConductorId(Long): UnidadTransporte y Conductor usan
    // ahora "codigo" (String) como PK.
    List<AsignacionTurno> findByUnidadCodigo(String unidadCodigo);
    List<AsignacionTurno> findByConductorCodigo(String conductorCodigo);
}
