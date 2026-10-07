package com.ecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para todos los controladores REST de la aplicación.
 * <p>
 * Captura excepciones del tipo {@link ApiException}, errores de validación de campos
 * {@link MethodArgumentNotValidException} y cualquier error no controlado,
 * retornando una estructura JSON estandarizada con marca de tiempo, código de estado,
 * mensaje y ruta de la petición.
 * </p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Captura las excepciones de negocio lanzadas mediante {@link ApiException}.
     *
     * @param ex      excepción capturada.
     * @param request petición HTTP actual.
     * @return respuesta HTTP con el código de estado y JSON descriptivo.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", ex.getStatus().value(),
                "error", ex.getStatus().getReasonPhrase(),
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        ));
    }

    /**
     * Captura los errores de validación de argumentos annotated con {@code @Valid}.
     *
     * @param ex      excepción de validación.
     * @param request petición HTTP actual.
     * @return respuesta HTTP 400 Bad Request con el mensaje del primer campo erróneo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", message,
                "path", request.getRequestURI()
        ));
    }

    /**
     * Denegaciones de {@code @PreAuthorize} (rol insuficiente). Sin este manejador caerían en el
     * manejador genérico y se convertirían en un 500.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 403,
                "error", "Forbidden",
                "message", "No tienes permisos para esta operación",
                "path", request.getRequestURI()
        ));
    }

    /**
     * Captura cualquier excepción no controlada o inesperada producida en el servidor.
     *
     * @param ex      excepción inesperada.
     * @param request petición HTTP actual.
     * @return respuesta HTTP 500 Internal Server Error con mensaje genérico de error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "Ocurrió un error interno. Revisa la configuración del servidor.",
                "path", request.getRequestURI()
        ));
    }
}