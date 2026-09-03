package com.pedidos360.inventory.service;

import java.util.List;

import com.pedidos360.inventory.domain.MovementType;
import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.domain.StockMovement;
import com.pedidos360.inventory.repository.ProductRepository;
import com.pedidos360.inventory.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimientos de stock. Cada llamada a {@link #registerMovement} ajusta el
 * stock del producto y deja un {@link StockMovement} de auditoria, todo en la
 * misma transaccion.
 */
@Service
@Transactional
public class InventoryService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public InventoryService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> history(Long productId) {
        if (!products.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return movements.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public StockMovement registerMovement(Long productId, MovementType type, int quantity,
                                          String reason, String createdBy) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        applyToStock(product, type, quantity);

        StockMovement movement = new StockMovement(
                product, type, quantity, product.getStock(), reason, createdBy);
        return movements.save(movement);
        // el stock del producto se persiste por dirty checking al cerrar la transaccion
    }

    private void applyToStock(Product product, MovementType type, int quantity) {
        switch (type) {
            case IN -> product.changeStock(quantity);
            case OUT -> {
                if (quantity > product.getStock()) {
                    throw new InsufficientStockException(product.getSku(), product.getStock(), quantity);
                }
                product.changeStock(-quantity);
            }
            case ADJUSTMENT -> product.setStock(quantity);
        }
    }
}
