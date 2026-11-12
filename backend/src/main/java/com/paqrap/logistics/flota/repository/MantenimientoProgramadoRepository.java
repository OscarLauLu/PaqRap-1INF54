package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.MantenimientoProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MantenimientoProgramadoRepository extends JpaRepository<MantenimientoProgramado, String> {

    /** Trae la unidad junto con el registro (la relación es LAZY y se lee fuera de transacción). */
    @Query("select m from MantenimientoProgramado m join fetch m.unidad")
    List<MantenimientoProgramado> findAllConUnidad();
}
