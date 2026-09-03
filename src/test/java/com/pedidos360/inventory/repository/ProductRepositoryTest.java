package com.pedidos360.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.pedidos360.inventory.domain.Product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * Test de la capa de repositorio contra H2 embebida: comprueba los finders
 * derivados y la @Query de bajo stock.
 */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository products;

    private Product nuevo(String sku, int stock, int minStock, boolean active) {
        Product p = new Product(sku, "Producto " + sku, "Test", "unidad", new BigDecimal("100.00"), minStock);
        p.setStock(stock);
        p.setActive(active);
        return p;
    }

    @Test
    void findBySku_devuelveElProducto() {
        products.save(nuevo("SKU-1", 10, 5, true));

        assertThat(products.findBySku("SKU-1")).isPresent();
        assertThat(products.findBySku("NOPE")).isEmpty();
        assertThat(products.existsBySku("SKU-1")).isTrue();
    }

    @Test
    void findByActiveTrue_excluyeInactivos() {
        products.save(nuevo("ACT-1", 10, 5, true));
        products.save(nuevo("INA-1", 10, 5, false));

        List<Product> activos = products.findByActiveTrueOrderByNameAsc();

        assertThat(activos).extracting(Product::getSku).containsExactly("ACT-1");
    }

    @Test
    void findLowStock_soloActivosEnOPorDebajoDelUmbral() {
        products.save(nuevo("LOW-1", 3, 5, true));    // bajo
        products.save(nuevo("LOW-2", 5, 5, true));    // en el umbral -> bajo
        products.save(nuevo("OK-1", 20, 5, true));    // ok
        products.save(nuevo("LOW-INA", 0, 5, false)); // bajo pero inactivo -> fuera

        assertThat(products.findLowStock())
                .extracting(Product::getSku)
                .containsExactly("LOW-1", "LOW-2");
    }
}
