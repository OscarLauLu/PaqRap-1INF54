package com.paqrap.logistics.almacen.repository;

import com.paqrap.logistics.almacen.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, String> {
    Optional<Almacen> findByCodigo(String codigo);
    Optional<Almacen> findByNombreIgnoreCase(String nombre);
}
