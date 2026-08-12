package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.entity.Cliente;
import com.ecommerce.service.CarritoService;
import com.ecommerce.util.SessionGuard;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {
    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public CarritoResponse get(HttpSession session) {
        Cliente cliente = SessionGuard.requireClient(session);
        return CarritoResponse.from(carritoService.getOrCreate(cliente));
    }

    @PostMapping
    public CarritoResponse add(@Valid @RequestBody CarritoRequest request, HttpSession session) {
        return CarritoResponse.from(carritoService.add(SessionGuard.requireClient(session), request));
    }

    @PutMapping("/detalle/{id}")
    public CarritoResponse update(@PathVariable Long id, @Valid @RequestBody CarritoRequest request,
                                  HttpSession session) {
        return CarritoResponse.from(carritoService.update(SessionGuard.requireClient(session), id, request));
    }

    @DeleteMapping("/detalle/{id}")
    public CarritoResponse remove(@PathVariable Long id, HttpSession session) {
        return CarritoResponse.from(carritoService.remove(SessionGuard.requireClient(session), id));
    }

    @DeleteMapping
    public Map<String, String> clear(HttpSession session) {
        carritoService.clear(SessionGuard.requireClient(session));
        return Map.of("message", "Carrito vaciado");
    }
}