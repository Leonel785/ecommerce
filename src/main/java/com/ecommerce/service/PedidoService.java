package com.ecommerce.service;

import com.ecommerce.entity.*;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final CarritoService carritoService;
    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository, CarritoService carritoService,
                         CarritoRepository carritoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.carritoService = carritoService;
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public Pedido confirm(Cliente cliente) {
        Carrito carrito = carritoService.getOrCreate(cliente);
        if (carrito.getDetalles().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "No puedes confirmar un carrito vacío");
        }
        for (DetalleCarrito detalle : carrito.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            if (detalle.getCantidad() > producto.getStock()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre());
            }
        }
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PAGADO);
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCarrito detalle : carrito.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() - detalle.getCantidad());
            productoRepository.save(producto);
            DetallePedido detallePedido = new DetallePedido(pedido, producto, detalle.getCantidad(),
                    detalle.getPrecioUnitario());
            pedido.getDetalles().add(detallePedido);
            total = total.add(detallePedido.getSubtotal());
        }
        pedido.setTotal(total);
        Pedido saved = pedidoRepository.save(pedido);
        carrito.getDetalles().clear();
        carrito.setTotal(BigDecimal.ZERO);
        carritoRepository.save(carrito);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Pedido> findFor(Cliente cliente) {
        List<Pedido> pedidos = pedidoRepository.findByClienteOrderByFechaDesc(cliente);
        pedidos.forEach(p -> p.getDetalles().size());
        return pedidos;
    }

    @Transactional(readOnly = true)
    public Pedido findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        pedido.getDetalles().size();
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> findAll() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        pedidos.forEach(p -> p.getDetalles().size());
        return pedidos;
    }

    @Transactional
    public Pedido updateEstado(Long id, EstadoPedido estado) {
        Pedido pedido = findById(id);
        pedido.setEstado(estado);
        return pedidoRepository.save(pedido);
    }
}