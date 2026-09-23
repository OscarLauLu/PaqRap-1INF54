package com.paqrap.logistics.planificacion.repository;

import com.paqrap.logistics.planificacion.model.EstadoRuta;
import com.paqrap.logistics.planificacion.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {
    Optional<Ruta> findByCodigo(String codigo);
    List<Ruta> findByEstado(EstadoRuta estado);
    // Antes findByUnidadTransporteId(Long): UnidadTransporte usa ahora "codigo" (String) como PK.
    List<Ruta> findByUnidadTransporteCodigo(String unidadCodigo);
}
