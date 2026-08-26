package com.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción personalizada para gestionar errores de negocio en la API REST.
 * <p>
 * Incluye un código de estado HTTP ({@link HttpStatus}) y un mensaje explicativo
 * que es retornado al cliente por el controlador global de excepciones {@link ApiExceptionHandler}.
 * </p>
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    /**
     * Construye una nueva excepción de la API.
     *
     * @param status  código de estado HTTP a responder (ej. 400 BAD_REQUEST, 404 NOT_FOUND).
     * @param message mensaje descriptivo del error.
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Devuelve el estado HTTP asociado a la excepción.
     *
     * @return estado HTTP.
     */
    public HttpStatus getStatus() {
        return status;
    }
}