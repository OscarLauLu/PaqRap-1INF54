package com.paqrap.logistics.flota.repository;

import com.paqrap.logistics.flota.model.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoVehiculoRepository extends JpaRepository<TipoVehiculo, String> {
    Optional<TipoVehiculo> findByNombreIgnoreCase(String nombre);
}
