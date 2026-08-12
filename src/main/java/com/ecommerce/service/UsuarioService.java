package com.ecommerce.service;

import com.ecommerce.dto.RegistroRequest;
import com.ecommerce.entity.Carrito;
import com.ecommerce.entity.Cliente;
import com.ecommerce.entity.Rol;
import com.ecommerce.entity.Usuario;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.CarritoRepository;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final CarritoRepository carritoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          CarritoRepository carritoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.carritoRepository = carritoRepository;
    }

    public Usuario authenticate(String username, String password) {
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        if (!user.getPassword().equals(password)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        return user;
    }

    @Transactional
    public Cliente register(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.username().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
        }
        if (clienteRepository.findByCorreo(request.correo().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        Usuario usuario = new Usuario(
                request.username().trim(),
                request.password(),
                Rol.CLIENTE
        );
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente(
                request.nombres().trim(),
                request.correo().trim(),
                request.direccion() != null ? request.direccion().trim() : "",
                request.telefono() != null ? request.telefono().trim() : "",
                usuario
        );
        cliente = clienteRepository.save(cliente);

        Carrito carrito = new Carrito(cliente);
        carritoRepository.save(carrito);
        cliente.setCarrito(carrito);

        return cliente;
    }
}