package com.ecommerce.service;

import com.ecommerce.dto.CarritoRequest;
import com.ecommerce.entity.*;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarritoService {
    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;

    public CarritoService(CarritoRepository carritoRepository, ProductoRepository productoRepository) {
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public Carrito getOrCreate(Cliente cliente) {
        Carrito carrito = carritoRepository.findByCliente(cliente)
                .orElseGet(() -> carritoRepository.save(new Carrito(cliente)));
        carrito.getDetalles().size();
        return carrito;
    }

    @Transactional
    public Carrito add(Cliente cliente, CarritoRequest request) {
        if (request.cantidad() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a 0");
        }
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        Carrito carrito = getOrCreate(cliente);
        DetalleCarrito detalle = carrito.getDetalles().stream()
                .filter(d -> d.getProducto().getId().equals(producto.getId()))
                .findFirst().orElse(null);
        int nuevaCantidad = request.cantidad() + (detalle == null ? 0 : detalle.getCantidad());
        if (nuevaCantidad > producto.getStock()) {
            throw new ApiException(HttpStatus.CONFLICT, "La cantidad solicitada supera el stock disponible");
        }
        if (detalle == null) {
            detalle = new DetalleCarrito(carrito, producto, request.cantidad());
            carrito.getDetalles().add(detalle);
        } else {
            detalle.setCantidad(nuevaCantidad);
            detalle.recalculate();
        }
        recalculate(carrito);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito update(Cliente cliente, Long detalleId, CarritoRequest request) {
        Carrito carrito = getOrCreate(cliente);
        DetalleCarrito detalle = findDetail(carrito, detalleId);
        if (request.cantidad() > detalle.getProducto().getStock()) {
            throw new ApiException(HttpStatus.CONFLICT, "La cantidad solicitada supera el stock disponible");
        }
        detalle.setCantidad(request.cantidad());
        detalle.recalculate();
        recalculate(carrito);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito remove(Cliente cliente, Long detalleId) {
        Carrito carrito = getOrCreate(cliente);
        DetalleCarrito detalle = findDetail(carrito, detalleId);
        carrito.getDetalles().remove(detalle);
        recalculate(carrito);
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito clear(Cliente cliente) {
        Carrito carrito = getOrCreate(cliente);
        carrito.getDetalles().clear();
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    private DetalleCarrito findDetail(Carrito carrito, Long detalleId) {
        return carrito.getDetalles().stream().filter(d -> d.getId().equals(detalleId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Detalle del carrito no encontrado"));
    }

    private void recalculate(Carrito carrito) {
        carrito.getDetalles().forEach(DetalleCarrito::recalculate);
        carrito.setTotal(carrito.getDetalles().stream().map(DetalleCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}