package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.EstadoOperativo;
import com.paqrap.logistics.flota.model.UnidadTransporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, String> {

    Optional<UnidadTransporte> findByCodigo(String codigo);

    // Antes findByActivoTrue(): el campo persistido pasó a llamarse "dadaDeBaja" (semántica invertida).
    List<UnidadTransporte> findByDadaDeBajaFalse();

    List<UnidadTransporte> findByEstadoOperativo(EstadoOperativo estado);

    // Antes findByActivoTrueAndEstadoOperativo(estado)
    List<UnidadTransporte> findByDadaDeBajaFalseAndEstadoOperativo(EstadoOperativo estado);

    List<UnidadTransporte> findByTipoNombreIgnoreCase(String tipoNombre);

    List<UnidadTransporte> findByTipoNombreIgnoreCaseAndEstadoOperativo(String tipoNombre, EstadoOperativo estado);
}
