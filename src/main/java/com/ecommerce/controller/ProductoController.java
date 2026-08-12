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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> all(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String categoria) {
        return productoService.search(buscar, categoria).stream()
                .map(ProductoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse one(@PathVariable Long id) {
        return ProductoResponse.from(productoService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse create(@Valid @RequestBody ProductoRequest request, HttpSession session) {
        SessionGuard.requireAdmin(session);
        return ProductoResponse.from(productoService.create(request));
    }

    @PutMapping("/{id}")
    public ProductoResponse update(@PathVariable Long id, @Valid @RequestBody ProductoRequest request,
                                   HttpSession session) {
        SessionGuard.requireAdmin(session);
        return ProductoResponse.from(productoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpSession session) {
        SessionGuard.requireAdmin(session);
        productoService.delete(id);
    }

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
        
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo debe tener una extensión válida");
        }
        
        String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        
        // 9. Validar formatos JPG, JPEG, PNG, WEBP
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".webp")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formatos de imagen permitidos: JPG, JPEG, PNG, WEBP");
        }
        
        String filename = UUID.randomUUID().toString() + ext;
        
        try {
            String baseDir = System.getProperty("user.dir");
            
            // Save to src/main/resources/static/uploads/productos/
            File srcFolder = new File(baseDir, "src/main/resources/static/uploads/productos");
            if (!srcFolder.exists()) {
                srcFolder.mkdirs();
            }
            File srcFile = new File(srcFolder, filename);
            file.transferTo(srcFile);
            
            // Copy to target/classes/static/uploads/productos/
            File targetFolder = new File(baseDir, "target/classes/static/uploads/productos");
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            File targetFile = new File(targetFolder, filename);
            Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar el archivo en el servidor: " + e.getMessage());
        }
        
        return Map.of("filename", filename);
    }
}