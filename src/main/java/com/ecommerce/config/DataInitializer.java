package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(UsuarioRepository usuarios, ClienteRepository clientes,
                           ProductoRepository productos, CarritoRepository carritos) {
        return args -> {
            if (usuarios.count() > 0) return;
            Usuario admin = usuarios.save(new Usuario("admin", "admin123", Rol.ADMIN));
            Usuario clienteUsuario = usuarios.save(new Usuario("cliente", "cliente123", Rol.CLIENTE));
            Cliente cliente = clientes.save(new Cliente("Cliente Demo", "cliente@demo.com",
                    "Av. Principal 123", "999 999 999", clienteUsuario));
            carritos.save(new Carrito(cliente));
            productos.save(new Producto("Laptop Pro 14", "Tecnología", new BigDecimal("2499.90"), 8, null));
            productos.save(new Producto("Audífonos Studio", "Audio", new BigDecimal("349.90"), 16, null));
            productos.save(new Producto("Mochila Urbana", "Accesorios", new BigDecimal("129.90"), 25, null));
            productos.save(new Producto("Teclado Mecánico", "Tecnología", new BigDecimal("289.90"), 12, null));
            productos.save(new Producto("Cámara Compacta", "Fotografía", new BigDecimal("899.00"), 5, null));
        };
    }
}