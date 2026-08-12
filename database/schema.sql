CREATE DATABASE IF NOT EXISTS ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce;

-- Hibernate crea/actualiza estas tablas a partir de las entidades.
-- Este script documenta el modelo base solicitado por el docente.
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombres VARCHAR(140) NOT NULL,
    correo VARCHAR(160) NOT NULL UNIQUE,
    direccion VARCHAR(220),
    telefono VARCHAR(40),
    usuario_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS producto (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(160) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL,
    imagen VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS carrito (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha DATETIME NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    cliente_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_carrito_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE IF NOT EXISTS detalle_carrito (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    carrito_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    CONSTRAINT fk_detalle_carrito_carrito FOREIGN KEY (carrito_id) REFERENCES carrito(id),
    CONSTRAINT fk_detalle_carrito_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS pedido (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fecha DATETIME NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    cliente_id BIGINT NOT NULL,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    CONSTRAINT fk_detalle_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id),
    CONSTRAINT fk_detalle_pedido_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);