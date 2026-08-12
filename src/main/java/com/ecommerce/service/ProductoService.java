package com.ecommerce.service;

import com.ecommerce.dto.ProductoRequest;
import com.ecommerce.entity.Producto;
import com.ecommerce.exception.ApiException;
import com.ecommerce.repository.ProductoRepository;
import java.io.File;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {
    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

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

    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

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

    @Transactional
    public void delete(Long id) {
        Producto producto = findById(id);
        String imagen = producto.getImagen();
        productoRepository.delete(producto);
        deleteImageFile(imagen);
    }

    private void deleteImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return;
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