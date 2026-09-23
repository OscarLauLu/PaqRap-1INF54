package com.paqrap.logistics.almacen.repository;

import com.paqrap.logistics.almacen.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, String> {
    // Antes findByAlmacenId...(Long): Almacen usa ahora "codigo" (String) como PK.
    List<MovimientoInventario> findByAlmacenCodigoOrderByFechaHoraDesc(String almacenCodigo);
    List<MovimientoInventario> findByAlmacenCodigoAndFechaHoraBetweenOrderByFechaHoraDesc(
            String almacenCodigo, LocalDateTime desde, LocalDateTime hasta);
}
