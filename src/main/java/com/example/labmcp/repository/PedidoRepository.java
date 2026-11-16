package com.example.labmcp.repository;

import com.example.labmcp.model.EstadoPedido;
import com.example.labmcp.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    //Listar los pedidos de un cliente
    List<Pedido> findByClienteId(Long clienteId);

    //Listar los pedidos por estado
    List<Pedido> findByEstado(EstadoPedido estado);
}