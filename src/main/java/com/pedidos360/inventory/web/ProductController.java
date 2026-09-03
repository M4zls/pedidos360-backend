package com.pedidos360.inventory.web;

import java.net.URI;
import java.util.List;

import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.service.ProductService;
import com.pedidos360.inventory.web.dto.CreateProductRequest;
import com.pedidos360.inventory.web.dto.ProductResponse;
import com.pedidos360.inventory.web.dto.UpdateProductRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del microservicio de inventario: catalogo de productos. Todas las rutas
 * quedan bajo {@code /api/inventory} y requieren un Bearer token valido (ver
 * SecurityConfig -> anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/inventory")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/products")
    public List<ProductResponse> list(@RequestParam(required = false) String category) {
        return products.list(category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return products.lowStock().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(products.get(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest body) {
        Product created = products.create(
                body.sku(), body.name(), body.category(), body.unit(),
                body.unitPrice(), body.minStock(), body.initialStock());
        return ResponseEntity
                .created(URI.create("/api/inventory/products/" + created.getId()))
                .body(ProductResponse.from(created));
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest body) {
        Product updated = products.update(
                id, body.name(), body.category(), body.unit(),
                body.unitPrice(), body.minStock(), body.active());
        return ProductResponse.from(updated);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        products.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
