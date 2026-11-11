package com.paqrap.logistics.simulacion.repository;

import com.paqrap.logistics.simulacion.model.ResultadoSimulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoSimulacionRepository extends JpaRepository<ResultadoSimulacion, Long> {
    Optional<ResultadoSimulacion> findByCodigo(String codigo);
    List<ResultadoSimulacion> findBySessionId(String sessionId);
}

