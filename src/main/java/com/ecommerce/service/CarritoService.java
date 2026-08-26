package com.ecommerce.service;

import com.ecommerce.dto.CarritoRequest;
import com.ecommerce.entity.*;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para la gestión del carrito de compras.
 * <p>
 * Encapsula las operaciones de alta, modificación, eliminación y consulta de
 * los detalles del carrito, además del recálculo del total y la validación de
 * stock contra el catálogo de productos.
 * </p>
 */
@Service
public class CarritoService {
    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;

    public CarritoService(CarritoRepository carritoRepository, ProductoRepository productoRepository) {
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
    }

    /**
     * Obtiene el carrito del cliente. Si el cliente aún no tiene uno asociado,
     * se crea uno nuevo vacío. Además, fuerza la inicialización de la colección
     * de detalles para evitar problemas de LazyInitialization fuera de la sesión.
     *
     * @param cliente cliente dueño del carrito.
     * @return carrito existente o recién creado.
     */
    @Transactional
    public Carrito getOrCreate(Cliente cliente) {
        Carrito carrito = carritoRepository.findByCliente(cliente)
                .orElseGet(() -> carritoRepository.save(new Carrito(cliente)));
        carrito.getDetalles().size();
        return carrito;
    }

    /**
     * Agrega un producto al carrito del cliente. Si el producto ya estaba en
     * el carrito, se suma la cantidad solicitada. Valida que la cantidad
     * acumulada no supere el stock disponible del producto.
     *
     * @param cliente cliente autenticado.
     * @param request datos del producto y cantidad a agregar.
     * @return carrito actualizado y persistido.
     * @throws ApiException si la cantidad no es válida, el producto no existe
     *                     o se supera el stock disponible.
     */
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

    /**
     * Actualiza la cantidad de un detalle existente del carrito.
     * <p>Si la nueva cantidad supera el stock disponible, se rechaza la operación.</p>
     *
     * @param cliente   cliente dueño del carrito.
     * @param detalleId identificador del {@code DetalleCarrito} a modificar.
     * @param request   datos con la nueva cantidad.
     * @return carrito actualizado.
     * @throws ApiException si el detalle no existe o se supera el stock.
     */
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

    /**
     * Elimina un producto (detalle) del carrito del cliente.
     *
     * @param cliente   cliente dueño del carrito.
     * @param detalleId identificador del {@code DetalleCarrito} a eliminar.
     * @return carrito resultante tras la eliminación.
     * @throws ApiException si el detalle no existe en el carrito.
     */
    @Transactional
    public Carrito remove(Cliente cliente, Long detalleId) {
        Carrito carrito = getOrCreate(cliente);
        DetalleCarrito detalle = findDetail(carrito, detalleId);
        carrito.getDetalles().remove(detalle);
        recalculate(carrito);
        return carritoRepository.save(carrito);
    }

    /**
     * Vacía por completo el carrito del cliente, eliminando todos los detalles
     * y reseteando el total a cero.
     *
     * @param cliente cliente dueño del carrito.
     * @return carrito vacío persistido.
     */
    @Transactional
    public Carrito clear(Cliente cliente) {
        Carrito carrito = getOrCreate(cliente);
        carrito.getDetalles().clear();
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }

    /**
     * Busca un detalle dentro del carrito por su identificador.
     *
     * @param carrito   carrito donde se buscará el detalle.
     * @param detalleId identificador del detalle buscado.
     * @return detalle encontrado.
     * @throws ApiException si el detalle no pertenece al carrito.
     */
    private DetalleCarrito findDetail(Carrito carrito, Long detalleId) {
        return carrito.getDetalles().stream().filter(d -> d.getId().equals(detalleId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Detalle del carrito no encontrado"));
    }

    /**
     * Recalcula el subtotal de cada detalle del carrito y actualiza el total
     * acumulado. Se invoca cada vez que se modifica la cantidad de un detalle
     * o se eliminan/agregan productos.
     *
     * @param carrito carrito cuyos totales serán recalculados.
     */
    private void recalculate(Carrito carrito) {
        carrito.getDetalles().forEach(DetalleCarrito::recalculate);
        carrito.setTotal(carrito.getDetalles().stream().map(DetalleCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}