package com.pedidos360.inventory;

import java.math.BigDecimal;
import java.util.List;

import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga productos de ejemplo la primera vez (cuando la tabla esta vacia).
 * En Java en vez de data.sql para no atarse al dialecto SQL de la base.
 */
@Component
class InventoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    private final ProductRepository products;

    InventoryDataInitializer(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }

        List<Product> sample = List.of(
                producto("HAR-0000", "Harina 0000", "Insumos", "kg", "980.00", 120, 40),
                producto("AZU-REF", "Azucar refinada", "Insumos", "kg", "870.00", 15, 25),
                producto("LEV-FRE", "Levadura fresca", "Insumos", "kg", "2100.00", 8, 10),
                producto("MAN-PAN", "Manteca pan", "Insumos", "kg", "3450.00", 30, 12),
                producto("CAF-GRA", "Cafe en grano", "Cafeteria", "kg", "9800.00", 22, 8),
                producto("MED-DOC", "Medialunas (docena)", "Producto", "docena", "4200.00", 40, 15),
                producto("PAN-MIG", "Pan de miga", "Producto", "unidad", "2600.00", 6, 10));

        products.saveAll(sample);
        log.info("Inventario: cargados {} productos de ejemplo", sample.size());
    }

    private static Product producto(String sku, String name, String category, String unit,
                                    String unitPrice, int stock, int minStock) {
        Product p = new Product(sku, name, category, unit, new BigDecimal(unitPrice), minStock);
        p.setStock(stock);
        return p;
    }
}
