package com.paqrap.logistics.pedidos.repository;

import com.paqrap.logistics.pedidos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String> {
    // idCliente ahora ES la PK; se conserva este método (válido como derived query sobre el
    // atributo idCliente) para no tocar los call sites existentes en PedidoService.
    Optional<Cliente> findByIdCliente(String idCliente);
}
