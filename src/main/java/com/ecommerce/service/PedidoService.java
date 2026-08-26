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

/**
 * Servicio de negocio para la gestión de pedidos.
 * <p>
 * Se encarga de confirmar pedidos a partir del carrito del cliente, validar
 * el stock, descontar las cantidades del inventario, y exponer operaciones
 * de consulta y actualización de estado para uso del cliente y del administrador.
 * </p>
 */
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

    /**
     * Convierte el carrito del cliente en un {@link Pedido} confirmado.
     * <p>Pasos ejecutados:</p>
     * <ol>
     *   <li>Valida que el carrito no esté vacío.</li>
     *   <li>Recorre cada detalle para confirmar que el stock del producto alcanza.</li>
     *   <li>Crea el pedido con estado {@link EstadoPedido#PAGADO} y fecha actual.</li>
     *   <li>Descuenta del stock de cada producto la cantidad comprada.</li>
     *   <li>Genera los {@code DetallePedido} correspondientes con precio e importes.</li>
     *   <li>Vacía el carrito del cliente.</li>
     * </ol>
     *
     * @param cliente cliente que confirma el pedido.
     * @return pedido persistido.
     * @throws ApiException si el carrito está vacío o el stock es insuficiente.
     */
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

    /**
     * Devuelve los pedidos del cliente, ordenados del más reciente al más antiguo.
     * Inicializa la colección de detalles para evitar excepciones de inicialización
     * diferida fuera de la sesión de persistencia.
     *
     * @param cliente cliente del que se obtienen los pedidos.
     * @return lista de pedidos del cliente.
     */
    @Transactional(readOnly = true)
    public List<Pedido> findFor(Cliente cliente) {
        List<Pedido> pedidos = pedidoRepository.findByClienteOrderByFechaDesc(cliente);
        pedidos.forEach(p -> p.getDetalles().size());
        return pedidos;
    }

    /**
     * Busca un pedido por su identificador, inicializando su colección de detalles.
     *
     * @param id identificador del pedido.
     * @return pedido encontrado.
     * @throws ApiException si no existe un pedido con el id indicado.
     */
    @Transactional(readOnly = true)
    public Pedido findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        pedido.getDetalles().size();
        return pedido;
    }

    /**
     * Devuelve todos los pedidos del sistema. Uso exclusivo del panel
     * administrativo.
     *
     * @return lista completa de pedidos.
     */
    @Transactional(readOnly = true)
    public List<Pedido> findAll() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        pedidos.forEach(p -> p.getDetalles().size());
        return pedidos;
    }

    /**
     * Cambia el estado de un pedido (por ejemplo, marcarlo como
     * {@code ENVIADO} o {@code ENTREGADO}).
     *
     * @param id     identificador del pedido.
     * @param estado nuevo estado a aplicar.
     * @return pedido actualizado.
     * @throws ApiException si el pedido no existe.
     */
    @Transactional
    public Pedido updateEstado(Long id, EstadoPedido estado) {
        Pedido pedido = findById(id);
        pedido.setEstado(estado);
        return pedidoRepository.save(pedido);
    }
}