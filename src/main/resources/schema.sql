-- ============================================================
-- Script de creación de la base de datos `tareas`
-- Ejecutar una sola vez antes de levantar el servidor MCP.
-- Ejecuta en WinPowerShell PS C:\Users\Diego\Downloads\tarea-mcp\tarea-mcp> mysql -u root -p -e "source C:/Users/Diego/Downloads/tarea-mcp/tarea-mcp/src/main/resources/schema.sql"
-- ============================================================

CREATE DATABASE IF NOT EXISTS lab3
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lab3;

-- Crear la base de datos (ejecutar una vez, desde psql conectado a postgres)
-- CREATE DATABASE supermercado;
-- \c supermercado

CREATE TABLE IF NOT EXISTS cliente (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL,
    direccion  VARCHAR(255) NOT NULL,
    telefono   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS producto (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(120) NOT NULL,
    precio  NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    stock   INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS pedido (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT NOT NULL REFERENCES cliente(id),
    fecha       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                CHECK (estado IN ('PENDIENTE','EN_CAMINO','ENTREGADO','CANCELADO')),
    total       NUMERIC(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    pedido_id        BIGINT NOT NULL REFERENCES pedido(id),
    producto_id      BIGINT NOT NULL REFERENCES producto(id),
    cantidad         INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario  NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (pedido_id, producto_id)
);

-- Datos de prueba
INSERT INTO cliente (nombre, direccion, telefono) VALUES
('Ana Pérez',  'Cra 50 #20-10, Itagüí',    '3001112233'),
('Luis Gómez', 'Calle 3 #45-20, Medellín', '3004445566');

INSERT INTO producto (nombre, precio, stock) VALUES
('Leche entera 1L',       4500, 50),
('Pan tajado integral',   6200, 20),
('Huevos x12',           15900, 30),
('Arroz 500g',            3200, 40),
('Aceite de girasol 1L', 13500, 15),
('Café molido 250g',     11800, 25);