package com.pedidos360.inventory.service;

import java.util.List;

import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Reglas de negocio de los productos del inventario. Orquesta la capa de
 * repositorio ({@link ProductRepository}); los controllers no la tocan.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> list(String category) {
        if (StringUtils.hasText(category)) {
            return products.findByActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(category.trim());
        }
        return products.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> lowStock() {
        return products.findLowStock();
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(String sku, String name, String category, String unit,
                          java.math.BigDecimal unitPrice, int minStock, int initialStock) {
        String normalizedSku = sku.trim().toUpperCase();
        if (products.existsBySku(normalizedSku)) {
            throw new DuplicateSkuException(normalizedSku);
        }
        Product product = new Product(normalizedSku, name.trim(), trimToNull(category), unit.trim(), unitPrice, minStock);
        if (initialStock > 0) {
            product.setStock(initialStock);
        }
        return products.save(product);
    }

    public Product update(Long id, String name, String category, String unit,
                          java.math.BigDecimal unitPrice, int minStock, boolean active) {
        Product product = get(id);
        product.setName(name.trim());
        product.setCategory(trimToNull(category));
        product.setUnit(unit.trim());
        product.setUnitPrice(unitPrice);
        product.setMinStock(minStock);
        product.setActive(active);
        return product; // dirty checking dentro de la transaccion
    }

    /** Baja logica: se conserva el historial de movimientos. */
    public void deactivate(Long id) {
        get(id).setActive(false);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
