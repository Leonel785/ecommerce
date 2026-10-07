package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.entity.Cliente;
import com.ecommerce.service.CarritoService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.security.AuthUser;
import com.ecommerce.security.CurrentClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Controlador REST para la gestión del carrito de compras del cliente autenticado.
 * <p>
 * Todos los endpoints requieren una sesión activa con rol {@code CLIENTE};
 * la verificación se realiza mediante {@code @PreAuthorize("hasRole('CLIENTE')")} junto con las reglas de {@code SecurityConfig}.
 * </p>
 *
 * <p>Endpoints disponibles:</p>
 * <ul>
 *   <li>{@code GET    /api/carrito}             — Obtiene (o crea) el carrito del cliente.</li>
 *   <li>{@code POST   /api/carrito}             — Agrega un producto al carrito.</li>
 *   <li>{@code PUT    /api/carrito/detalle/{id}}— Actualiza la cantidad de un detalle.</li>
 *   <li>{@code DELETE /api/carrito/detalle/{id}}— Elimina un producto del carrito.</li>
 *   <li>{@code DELETE /api/carrito}             — Vacía el carrito completo.</li>
 * </ul>
 */
@PreAuthorize("hasRole('CLIENTE')")
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {
    private final CarritoService carritoService;
    private final CurrentClient currentClient;

    public CarritoController(CarritoService carritoService, CurrentClient currentClient) {
        this.carritoService = carritoService;
        this.currentClient = currentClient;
    }

    /**
     * Obtiene el carrito del cliente autenticado. Si el cliente aún no tiene
     * un carrito, se crea uno vacío automáticamente.
     *
     * @return carrito con la lista de detalles y el total calculado.
     */
    @GetMapping
    public CarritoResponse get(@AuthenticationPrincipal AuthUser user) {
        Cliente cliente = currentClient.from(user);
        return CarritoResponse.from(carritoService.getOrCreate(cliente));
    }

    /**
     * Agrega un producto al carrito. Si el producto ya existe en el carrito,
     * se incrementa la cantidad. Valida que la cantidad solicitada no supere
     * el stock disponible.
     *
     * @param request cuerpo con el id del producto y la cantidad a agregar.
     * @return carrito actualizado con el nuevo detalle.
     */
    @PostMapping
    public CarritoResponse add(@Valid @RequestBody CarritoRequest request, @AuthenticationPrincipal AuthUser user) {
        return CarritoResponse.from(carritoService.add(currentClient.from(user), request));
    }

    /**
     * Actualiza la cantidad de un producto específico dentro del carrito.
     * <p>Valida que la nueva cantidad no supere el stock disponible del producto.</p>
     *
     * @param id      identificador del {@code DetalleCarrito} a modificar.
     * @param request cuerpo con la nueva cantidad del producto.
     * @return carrito actualizado.
     */
    @PutMapping("/detalle/{id}")
    public CarritoResponse update(@PathVariable Long id, @Valid @RequestBody CarritoRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return CarritoResponse.from(carritoService.update(currentClient.from(user), id, request));
    }

    /**
     * Elimina un producto específico del carrito del cliente.
     *
     * @param id      identificador del {@code DetalleCarrito} a eliminar.
     * @return carrito resultante tras la eliminación.
     */
    @DeleteMapping("/detalle/{id}")
    public CarritoResponse remove(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return CarritoResponse.from(carritoService.remove(currentClient.from(user), id));
    }

    /**
     * Vacía por completo el carrito del cliente autenticado, eliminando todos
     * sus detalles y reseteando el total a cero.
     *
     * @return mensaje confirmando la operación.
     */
    @DeleteMapping
    public Map<String, String> clear(@AuthenticationPrincipal AuthUser user) {
        carritoService.clear(currentClient.from(user));
        return Map.of("message", "Carrito vaciado");
    }
}