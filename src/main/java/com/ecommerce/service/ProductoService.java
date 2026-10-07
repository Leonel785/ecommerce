package com.ecommerce.service;

import com.ecommerce.dto.ProductoRequest;
import com.ecommerce.entity.Producto;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ProductoRepository;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para la gestión de productos del catálogo.
 * <p>
 * Centraliza las operaciones CRUD y la búsqueda de productos, además de
 * encargarse del mantenimiento de los archivos físicos de imagen cuando un
 * producto se crea, actualiza o elimina.
 * </p>
 */
@Service
public class ProductoService {
    /** Solo se permiten nombres generados por el sistema (UUID + extensión de imagen). */
    private static final Pattern SAFE_IMAGE_NAME = Pattern.compile("^[A-Za-z0-9-]{1,64}\\.(jpg|jpeg|png|webp)$");

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Devuelve todos los productos del catálogo.
     *
     * @return lista completa de productos.
     */
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    /**
     * Busca productos aplicando filtros combinables de texto y categoría.
     * <p>Reglas de filtrado:</p>
     * <ul>
     *   <li>Si se pasan ambos filtros, se aplican en conjunto (AND).</li>
     *   <li>Si sólo se pasa uno, se filtra por ese único criterio.</li>
     *   <li>Si no se pasa ninguno, se devuelven todos los productos.</li>
     * </ul>
     *
     * @param query    texto a buscar dentro del nombre (opcional).
     * @param category categoría exacta a filtrar (opcional).
     * @return lista de productos que cumplen los filtros.
     */
    public List<Producto> search(String query, String category) {
        if (query != null && !query.trim().isEmpty() && category != null && !category.trim().isEmpty()) {
            return productoRepository.findByNombreContainingIgnoreCaseAndCategoriaIgnoreCase(query.trim(), category.trim());
        } else if (query != null && !query.trim().isEmpty()) {
            return productoRepository.findByNombreContainingIgnoreCase(query.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            return productoRepository.findByCategoriaIgnoreCase(category.trim());
        } else {
            return productoRepository.findAll();
        }
    }

    /**
     * Busca un producto por su identificador.
     *
     * @param id identificador del producto.
     * @return producto encontrado.
     * @throws ApiException si no existe un producto con el id indicado.
     */
    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /**
     * Crea un nuevo producto a partir de los datos validados del request.
     * Los campos de texto se trimean para evitar valores con espacios sobrantes.
     *
     * @param request datos del producto a crear.
     * @return producto persistido.
     */
    @Transactional
    public Producto create(ProductoRequest request) {
        String imagen = (request.imagen() != null && !request.imagen().trim().isEmpty()) ? request.imagen().trim() : null;
        return productoRepository.save(new Producto(
                request.nombre().trim(),
                request.categoria().trim(),
                request.precio(),
                request.stock(),
                imagen
        ));
    }

    /**
     * Actualiza los datos de un producto existente. Si la imagen del producto cambia
     * o se remueve, elimina el archivo de imagen antiguo del sistema de archivos.
     *
     * @param id      identificador del producto a actualizar.
     * @param request nuevos datos del producto.
     * @return producto actualizado y persistido.
     * @throws ApiException si el producto no existe.
     */
    @Transactional
    public Producto update(Long id, ProductoRequest request) {
        Producto producto = findById(id);
        producto.setNombre(request.nombre().trim());
        producto.setCategoria(request.categoria().trim());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        
        String oldImagen = producto.getImagen();
        String newImagen = (request.imagen() != null && !request.imagen().trim().isEmpty()) ? request.imagen().trim() : null;
        
        if (newImagen != null && !newImagen.equals(oldImagen)) {
            deleteImageFile(oldImagen);
            producto.setImagen(newImagen);
        } else if (request.imagen() == null || request.imagen().trim().isEmpty()) {
            if (oldImagen != null) {
                deleteImageFile(oldImagen);
                producto.setImagen(null);
            }
        }
        
        return productoRepository.save(producto);
    }

    /**
     * Elimina un producto de la base de datos y remueve su archivo de imagen asociado.
     *
     * @param id identificador del producto a eliminar.
     * @throws ApiException si el producto no existe.
     */
    @Transactional
    public void delete(Long id) {
        Producto producto = findById(id);
        String imagen = producto.getImagen();
        productoRepository.delete(producto);
        deleteImageFile(imagen);
    }

    /**
     * Elimina físicamente un archivo de imagen guardado en el servidor,
     * tanto en la carpeta de recursos de origen {@code src/...} como en la
     * carpeta de clases compiladas {@code target/...}.
     *
     * @param filename nombre del archivo a eliminar.
     */
    private void deleteImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return;
        // Defensa contra path traversal (p. ej. "../../etc/x") y protección de la imagen por defecto.
        if (!SAFE_IMAGE_NAME.matcher(filename).matches() || "default.jpg".equals(filename)) return;
        try {
            String baseDir = System.getProperty("user.dir");
            
            // Delete from src
            File srcFile = new File(baseDir, "src/main/resources/static/uploads/productos/" + filename);
            if (srcFile.exists()) {
                srcFile.delete();
            }
            
            // Delete from target
            File targetFile = new File(baseDir, "target/classes/static/uploads/productos/" + filename);
            if (targetFile.exists()) {
                targetFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Error deleting file " + filename + ": " + e.getMessage());
        }
    }
}