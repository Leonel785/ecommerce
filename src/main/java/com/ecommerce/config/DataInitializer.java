package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.security.CryptoService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicialización de datos al arrancar la aplicación (solo si la tabla de usuarios está vacía).
 * <p>
 * Ya no existen credenciales fijas conocidas: la contraseña del administrador se toma de
 * {@code ADMIN_PASSWORD} o, si no se define, se genera aleatoriamente y se muestra UNA vez en
 * el log de arranque. El cliente de demostración solo se crea con {@code SEED_DEMO_USER=true}.
 * </p>
 */
@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seed(UsuarioRepository usuarios, ClienteRepository clientes,
                           ProductoRepository productos, CarritoRepository carritos,
                           PasswordEncoder encoder, CryptoService crypto,
                           @Value("${app.seed.admin-password:}") String adminPassword,
                           @Value("${app.seed.demo-user:false}") boolean seedDemoUser) {
        return args -> {
            if (usuarios.count() > 0) return;

            String adminPwd = adminPassword;
            if (adminPwd == null || adminPwd.isBlank()) {
                adminPwd = randomPassword();
                log.warn("=== CREDENCIAL INICIAL (se muestra una sola vez) === usuario: admin | contraseña: {}", adminPwd);
            }
            usuarios.save(new Usuario("admin", encoder.encode(adminPwd), Rol.ADMIN));

            if (seedDemoUser) {
                String demoPwd = randomPassword();
                Usuario clienteUsuario = usuarios.save(new Usuario("cliente", encoder.encode(demoPwd), Rol.CLIENTE));
                Cliente cliente = new Cliente("Cliente Demo", "cliente@demo.com",
                        "Av. Principal 123", "999 999 999", clienteUsuario);
                cliente.setCorreoHash(crypto.blindIndex(cliente.getCorreo()));
                cliente = clientes.save(cliente);
                carritos.save(new Carrito(cliente));
                log.warn("=== CLIENTE DEMO (se muestra una sola vez) === usuario: cliente | contraseña: {}", demoPwd);
            }

            productos.save(new Producto("Laptop Pro 14", "Tecnología", new BigDecimal("2499.90"), 8, null));
            productos.save(new Producto("Audífonos Studio", "Audio", new BigDecimal("349.90"), 16, null));
            productos.save(new Producto("Mochila Urbana", "Accesorios", new BigDecimal("129.90"), 25, null));
            productos.save(new Producto("Teclado Mecánico", "Tecnología", new BigDecimal("289.90"), 12, null));
            productos.save(new Producto("Cámara Compacta", "Fotografía", new BigDecimal("899.00"), 5, null));
        };
    }

    private static String randomPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 18; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
