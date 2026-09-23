package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, String> {
    Optional<Conductor> findByCodigo(String codigo);
    // findByActivoTrue() se eliminó: Conductor ya no tiene el campo "activo" (docs/62, sección 5).
}
