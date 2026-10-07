package com.ecommerce.config;

import com.ecommerce.entity.Cliente;
import com.ecommerce.repository.ClienteRepository;
import com.ecommerce.security.CryptoService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migración automática de datos existentes: los clientes creados antes de activar el cifrado
 * (sin {@code correo_hash}) se re-guardan, lo que escribe sus datos personales cifrados.
 * Es idempotente: si no hay registros pendientes no hace nada.
 */
@Component
@Transactional
public class EncryptionBackfill implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(EncryptionBackfill.class);

    private final ClienteRepository clientes;
    private final CryptoService crypto;

    public EncryptionBackfill(ClienteRepository clientes, CryptoService crypto) {
        this.clientes = clientes;
        this.crypto = crypto;
    }

    @Override
    public void run(String... args) {
        List<Cliente> pendientes = clientes.findAll().stream()
                .filter(c -> c.getCorreoHash() == null)
                .toList();
        // Al asignar el hash la entidad queda "sucia" y Hibernate reescribe todas sus columnas cifradas.
        pendientes.forEach(c -> c.setCorreoHash(crypto.blindIndex(c.getCorreo())));
        if (!pendientes.isEmpty()) {
            log.info("Cifrado de datos personales aplicado a {} cliente(s) existente(s).", pendientes.size());
        }
    }
}
