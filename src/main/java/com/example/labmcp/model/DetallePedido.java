package com.example.labmcp.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "detalle_pedido")
@IdClass(DetallePedido.DetalleId.class)
public class DetallePedido {

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    public DetallePedido() {}

    public DetallePedido(Pedido pedido, Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.pedido = pedido;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    // Clase de la clave compuesta (pedido_id, producto_id)
    public static class DetalleId implements Serializable {
        private Long pedido;
        private Long producto;

        public DetalleId() {}

        public DetalleId(Long pedido, Long producto) {
            this.pedido = pedido;
            this.producto = producto;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DetalleId other)) return false;
            return Objects.equals(pedido, other.pedido) && Objects.equals(producto, other.producto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pedido, producto);
        }
    }
}