package com.ecommerce.controller;

import com.ecommerce.dto.ProductoRequest;
import com.ecommerce.dto.ProductoResponse;
import com.ecommerce.exception.ApiException;
import com.ecommerce.service.ProductoService;
import com.ecommerce.util.SessionGuard;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para la gestión de productos del catálogo.
 * <p>
 * Los endpoints de búsqueda y consulta son públicos, mientras que la
 * creación, actualización, eliminación y carga de imágenes están restringidos
 * a usuarios con rol {@code ADMIN}.
 * </p>
 *
 * <p>Endpoints disponibles:</p>
 * <ul>
 *   <li>{@code GET    /api/productos}              — Lista / busca productos.</li>
 *   <li>{@code GET    /api/productos/{id}}         — Obtiene un producto por id.</li>
 *   <li>{@code POST   /api/productos}              — Crea un producto (admin).</li>
 *   <li>{@code PUT    /api/productos/{id}}         — Actualiza un producto (admin).</li>
 *   <li>{@code DELETE /api/productos/{id}}         — Elimina un producto (admin).</li>
 *   <li>{@code POST   /api/productos/upload}       — Sube la imagen de un producto (admin).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Lista los productos del catálogo permitiendo filtrar opcionalmente por
     * texto de búsqueda y/o categoría. Ambos filtros son combinables.
     *
     * @param buscar    texto parcial a buscar en el nombre (opcional).
     * @param categoria categoría exacta a filtrar (opcional).
     * @return lista de productos que cumplen los criterios.
     */
    @GetMapping
    public List<ProductoResponse> all(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String categoria) {
        return productoService.search(buscar, categoria).stream()
                .map(ProductoResponse::from).toList();
    }

    /**
     * Obtiene el detalle de un producto por su identificador.
     *
     * @param id identificador del producto.
     * @return producto encontrado.
     * @throws ApiException si no existe un producto con el id indicado.
     */
    @GetMapping("/{id}")
    public ProductoResponse one(@PathVariable Long id) {
        return ProductoResponse.from(productoService.findById(id));
    }

    /**
     * Crea un nuevo producto en el catálogo. Endpoint restringido a administradores.
     *
     * @param request datos del producto a crear.
     * @param session sesión HTTP del administrador.
     * @return producto creado.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse create(@Valid @RequestBody ProductoRequest request, HttpSession session) {
        SessionGuard.requireAdmin(session);
        return ProductoResponse.from(productoService.create(request));
    }

    /**
     * Actualiza los datos de un producto existente. Endpoint restringido a administradores.
     *
     * @param id      identificador del producto a actualizar.
     * @param request nuevos datos del producto.
     * @param session sesión HTTP del administrador.
     * @return producto actualizado.
     */
    @PutMapping("/{id}")
    public ProductoResponse update(@PathVariable Long id, @Valid @RequestBody ProductoRequest request,
                                   HttpSession session) {
        SessionGuard.requireAdmin(session);
        return ProductoResponse.from(productoService.update(id, request));
    }

    /**
     * Elimina un producto del catálogo. Endpoint restringido a administradores.
     *
     * @param id      identificador del producto a eliminar.
     * @param session sesión HTTP del administrador.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpSession session) {
        SessionGuard.requireAdmin(session);
        productoService.delete(id);
    }

    /**
     * Sube la imagen de un producto al servidor. Endpoint restringido a administradores.
     * <p>Reglas de validación aplicadas:</p>
     * <ul>
     *   <li>El archivo no puede estar vacío.</li>
     *   <li>El tamaño máximo permitido es de 5 MB.</li>
     *   <li>El archivo debe tener una extensión válida (.jpg, .jpeg, .png, .webp).</li>
     * </ul>
     * <p>El archivo se guarda tanto en {@code src/main/resources/static/uploads/productos/}
     * como en {@code target/classes/static/uploads/productos/} para que esté
     * disponible en tiempo de desarrollo y al ejecutar el JAR empaquetado.</p>
     *
     * @param file    archivo de imagen enviado en el campo {@code file}.
     * @param session sesión HTTP del administrador.
     * @return mapa con el nombre aleatorio generado para el archivo subido.
     * @throws ApiException si el archivo es inválido, demasiado grande o no se puede guardar.
     */
    @PostMapping("/upload")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file, HttpSession session) {
        SessionGuard.requireAdmin(session);

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        // 10. Limitar tamaño máximo de imagen a 5 MB.
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de la imagen supera el límite de 5 MB");
        }

        // No se confía en el nombre ni en el Content-Type enviados por el cliente:
        // el tipo real se determina por la firma binaria (magic bytes) del contenido.
        String ext;
        try (java.io.InputStream in = file.getInputStream()) {
            ext = detectImageExtension(in.readNBytes(12));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo enviado");
        }
        if (ext == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formatos de imagen permitidos: JPG, PNG, WEBP");
        }

        String filename = UUID.randomUUID().toString() + ext;

        try {
            String baseDir = System.getProperty("user.dir");

            // Persistimos en la carpeta de recursos del proyecto para que el archivo
            // quede disponible al servir estáticos durante el desarrollo.
            File srcFolder = new File(baseDir, "src/main/resources/static/uploads/productos");
            if (!srcFolder.exists()) {
                srcFolder.mkdirs();
            }
            File srcFile = new File(srcFolder, filename);
            file.transferTo(srcFile);

            // Duplicamos el archivo en la carpeta target/classes para que también
            // esté disponible al ejecutar la aplicación empaquetada (java -jar).
            File targetFolder = new File(baseDir, "target/classes/static/uploads/productos");
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            File targetFile = new File(targetFolder, filename);
            Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("Error al guardar la imagen del producto", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el archivo en el servidor");
        }

        return Map.of("filename", filename);
    }

    /**
     * Determina la extensión real de una imagen a partir de su firma binaria.
     *
     * @param h primeros bytes del archivo.
     * @return ".jpg", ".png", ".webp" o {@code null} si el contenido no es un formato permitido.
     */
    private static String detectImageExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return ".png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}