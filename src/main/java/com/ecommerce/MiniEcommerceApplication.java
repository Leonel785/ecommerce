package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de inicio para la aplicación Spring Boot MiniEcommerce.
 * <p>
 * Configura y arranca el contexto de Spring Boot, incluyendo componentes web,
 * repositorios Spring Data JPA y controladores REST / MVC.
 * </p>
 */
@SpringBootApplication
public class MiniEcommerceApplication {
    /**
     * Punto de entrada principal a la ejecución del servidor.
     *
     * @param args argumentos recibidos por línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(MiniEcommerceApplication.class, args);
    }
}