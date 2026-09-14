package com.pedidos360.inventory;

import java.math.BigDecimal;
import java.util.List;

import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga la carta de ejemplo la primera vez (cuando la tabla esta vacia).
 * En Java en vez de data.sql para no atarse al dialecto SQL de la base.
 */
@Component
@Order(10)
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

        String img = "https://images.unsplash.com/photo-";
        String crop = "?auto=format&fit=crop&w=640&q=70";

        List<Product> sample = List.of(
                item("BRG-NAAN", "Naan Burger", "Hamburguesas", "5200.00", 40, 12,
                        img + "1568901346375-23c9450c58cd" + crop),
                item("TAC-BUTTER", "Butter Chicken Taco", "Tacos", "4800.00", 35, 10,
                        img + "1551504734-5ee1c4a1479b" + crop),
                item("BRG-CHICKEN", "Chicken Burger", "Hamburguesas", "5600.00", 38, 12,
                        img + "1553979459-d2229ba7433b" + crop),
                item("SND-NAAN", "Cheese Chicken Naan", "Sándwiches", "4500.00", 30, 10,
                        img + "1601050690597-df0568f70950" + crop),
                item("BRG-5LAYER", "5 Layer Burger", "Hamburguesas", "7900.00", 25, 8,
                        img + "1571091718767-18b5b1457add" + crop),
                item("SND-CLASSIC", "Sandwich Clásico", "Sándwiches", "3800.00", 28, 10,
                        img + "1528735602780-2552fd46c7af" + crop),
                item("SID-FRIES", "Papas Clásicas", "Acompañamientos", "2500.00", 60, 20,
                        img + "1573080496219-bb080dd4f877" + crop),
                item("SID-ONION", "Aros de Cebolla", "Acompañamientos", "2800.00", 45, 15,
                        img + "1639024471283-03518883512d" + crop),
                item("DRK-COLA", "Coca-Cola", "Bebidas", "1500.00", 90, 30,
                        img + "1554866585-cd94860890b7" + crop),
                item("DRK-LEMON", "Limonada", "Bebidas", "1800.00", 50, 20,
                        img + "1621263764928-df1444c5e859" + crop));

        products.saveAll(sample);
        log.info("Inventario: cargada la carta de ejemplo ({} productos)", sample.size());
    }

    private static Product item(String sku, String name, String category,
                                String unitPrice, int stock, int minStock, String imageUrl) {
        Product p = new Product(sku, name, category, "unidad", new BigDecimal(unitPrice), minStock);
        p.setImageUrl(imageUrl);
        p.setStock(stock);
        return p;
    }
}
