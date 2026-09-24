package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio del catálogo de turnos (3 filas fijas: Mañana, Tarde, Noche).
 * Nuevo en este remodelado: Turno pasó de value object embebido a entidad propia
 * (docs/62.dis.base.datos.postgresql.v01.md, sección 2 "TABLAS CATÁLOGO").
 */
@Repository
public interface TurnoRepository extends JpaRepository<Turno, Integer> {
}
