package com.example.labmcp.repository;

import com.example.labmcp.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, DetallePedido.DetalleId> {

    // Todas las líneas de un pedido, para consultarPedido y para calcular el total
    List<DetallePedido> findByPedidoId(Long pedidoId);
}
