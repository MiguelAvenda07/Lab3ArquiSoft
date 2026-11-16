package com.example.labmcp.service;

import com.example.labmcp.model.*;
import com.example.labmcp.repository.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * Operaciones de pedidos de supermercado expuestas como herramientas MCP.
 *
 * Cada método @Tool se publica automáticamente por el servidor MCP:
 *   - nombre      = nombre del método
 *   - descripción = atributo description
 *   - JSON schema = derivado de los parámetros (incluye @ToolParam)
 *
 * Herramientas: buscarProductos, crearPedido, agregarProducto, consultarPedido,
 * actualizarEstadoPedido y cancelarPedido.
 *
 * Las reglas de negocio (transiciones de estado, stock y cálculo del total)
 * se validan aquí y no dependen de las instrucciones del agente.
 *
 * Clientes que consumen estas herramientas:
 *   - Claude Desktop, que recibe peticiones en lenguaje natural como
 *     "agrégame dos paquetes de leche al pedido 5".
 *   - El agente PedidoAgent de Google ADK, que usa las mismas herramientas
 *     desde la interfaz web en el puerto 8080.
 */
public class PedidoToolsService {

    private final ClienteRepository clienteRepo;
    private final ProductoRepository productoRepo;
    private final PedidoRepository pedidoRepo;
    private final DetallePedidoRepository detalleRepo;

    public PedidoToolsService(ClienteRepository clienteRepo,
                              ProductoRepository productoRepo,
                              PedidoRepository pedidoRepo,
                              DetallePedidoRepository detalleRepo) {
        this.clienteRepo = clienteRepo;
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
        this.detalleRepo = detalleRepo;
    }

    // DTOs de salida: evitan devolver entidades con relaciones LAZY
    // (con open-in-view=false, Jackson fallaría al serializarlas fuera de la transacción)
    public record LineaPedido(Long productoId, String producto, Integer cantidad,
                              BigDecimal precioUnitario, BigDecimal subtotal) {}

    public record PedidoDetalle(Long id, Long clienteId, String cliente, LocalDateTime fecha,
                                EstadoPedido estado, BigDecimal total, List<LineaPedido> lineas) {}

    @Tool(description = "Busca productos cuyo nombre contiene el fragmento indicado. Devuelve id, nombre, precio y stock.")
    @Transactional(readOnly = true)
    public List<Producto> buscarProductos(
            @ToolParam(description = "Texto a buscar en el nombre del producto") String fragmento) {
        if (fragmento == null || fragmento.isBlank()) {
            throw new IllegalArgumentException("El fragmento de búsqueda es obligatorio");
        }
        return productoRepo.findByNombreContainingIgnoreCase(fragmento.trim());
    }

    @Tool(description = "Crea un pedido nuevo en estado PENDIENTE para el cliente indicado por su id. Devuelve el pedido creado.")
    @Transactional
    public PedidoDetalle crearPedido(
            @ToolParam(description = "Id del cliente que realiza el pedido") Long clienteId) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(BigDecimal.ZERO);

        return aDetalle(pedidoRepo.save(pedido));
    }

    @Tool(description = "Agrega un producto al pedido indicado. El pedido debe estar PENDIENTE y debe haber stock suficiente. Si el producto ya está en el pedido, suma la cantidad.")
    @Transactional
    public PedidoDetalle agregarProducto(
            @ToolParam(description = "Id del pedido") Long pedidoId,
            @ToolParam(description = "Id del producto a agregar") Long productoId,
            @ToolParam(description = "Cantidad a agregar, mayor que cero") Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        Pedido pedido = obtenerPedido(pedidoId);
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalArgumentException("Solo se pueden modificar pedidos PENDIENTE; el pedido "
                    + pedidoId + " está " + pedido.getEstado());
        }

        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + productoId));
        if (producto.getStock() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                    + ": disponible " + producto.getStock());
        }

        // Se descuenta el stock al agregar; cancelarPedido lo devuelve
        producto.setStock(producto.getStock() - cantidad);

        // La clave (pedido_id, producto_id) es única: si la línea existe, se suma la cantidad
        DetallePedido.DetalleId id = new DetallePedido.DetalleId(pedidoId, productoId);
        DetallePedido linea = detalleRepo.findById(id)
                .orElseGet(() -> new DetallePedido(pedido, producto, 0, producto.getPrecio()));
        linea.setCantidad(linea.getCantidad() + cantidad);
        detalleRepo.save(linea);

        recalcularTotal(pedido);
        return aDetalle(pedido);
    }

    @Tool(description = "Consulta un pedido por su id: cliente, estado, total y líneas de productos.")
    @Transactional(readOnly = true)
    public PedidoDetalle consultarPedido(
            @ToolParam(description = "Id del pedido") Long pedidoId) {
        return aDetalle(obtenerPedido(pedidoId));
    }

    @Tool(description = "Cancela un pedido que no esté entregado y devuelve al stock las cantidades de sus productos. Confirme con el cliente antes de usarla.")
    @Transactional
    public PedidoDetalle cancelarPedido(
            @ToolParam(description = "Id del pedido a cancelar") Long pedidoId) {
        Pedido pedido = obtenerPedido(pedidoId);

        if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " ya fue entregado y no se puede cancelar");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            return aDetalle(pedido); // idempotente: cancelar dos veces no devuelve stock dos veces
        }

        for (DetallePedido d : detalleRepo.findByPedidoId(pedidoId)) {
            Producto p = d.getProducto();
            p.setStock(p.getStock() + d.getCantidad());
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        return aDetalle(pedidoRepo.save(pedido));
    }

    // ---------- Métodos auxiliares ----------

    private Pedido obtenerPedido(Long pedidoId) {
        return pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId));
    }

    private void recalcularTotal(Pedido pedido) {
        BigDecimal total = detalleRepo.findByPedidoId(pedido.getId()).stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);
        pedidoRepo.save(pedido);
    }

    private PedidoDetalle aDetalle(Pedido pedido) {
        List<LineaPedido> lineas = detalleRepo.findByPedidoId(pedido.getId()).stream()
                .map(d -> new LineaPedido(
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()))))
                .toList();

        return new PedidoDetalle(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNombre(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                lineas);
    }
}