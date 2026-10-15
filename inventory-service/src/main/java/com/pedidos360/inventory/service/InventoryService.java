package com.pedidos360.inventory.service;

import java.util.ArrayList;
import java.util.List;

import com.pedidos360.inventory.domain.MovementType;
import com.pedidos360.inventory.domain.Product;
import com.pedidos360.inventory.domain.StockAlert;
import com.pedidos360.inventory.domain.StockMovement;
import com.pedidos360.inventory.repository.ProductRepository;
import com.pedidos360.inventory.repository.StockAlertRepository;
import com.pedidos360.inventory.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimientos de stock. Cada llamada a {@link #registerMovement} ajusta el
 * stock del producto y deja un {@link StockMovement} de auditoria, todo en la
 * misma transaccion. Si el movimiento hace que el producto CRUCE a bajo
 * stock, ademas genera una {@link StockAlert}.
 */
@Service
@Transactional
public class InventoryService {

    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final StockAlertRepository alerts;

    public InventoryService(ProductRepository products, StockMovementRepository movements,
                            StockAlertRepository alerts) {
        this.products = products;
        this.movements = movements;
        this.alerts = alerts;
    }

    @Transactional(readOnly = true)
    public List<StockMovement> history(Long productId) {
        if (!products.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return movements.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public List<StockAlert> alerts() {
        return alerts.findAllByOrderByCreatedAtDesc();
    }

    public void markAlertRead(Long alertId) {
        StockAlert alert = alerts.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));
        alert.markRead();
    }

    public record OrderLineQuantity(Long productId, int quantity) {}

    /**
     * Descuenta el stock reservado por un pedido (una linea = un producto).
     * Primero valida que haya stock suficiente para TODAS las lineas antes de
     * tocar nada: si alguna no alcanza, no se aplica ningun movimiento (asi un
     * reintento del mensaje no deja el stock descontado a medias).
     */
    public void reserveStockForOrder(Long orderId, List<OrderLineQuantity> lines) {
        List<Product> locked = new ArrayList<>(lines.size());
        for (OrderLineQuantity line : lines) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new ProductNotFoundException(line.productId()));
            if (line.quantity() > product.getStock()) {
                throw new InsufficientStockException(product.getSku(), product.getStock(), line.quantity());
            }
            locked.add(product);
        }
        for (int i = 0; i < lines.size(); i++) {
            registerMovement(locked.get(i).getId(), MovementType.OUT, lines.get(i).quantity(),
                    "Reserva automatica por pedido #" + orderId, "rabbitmq-consumer");
        }
    }

    public StockMovement registerMovement(Long productId, MovementType type, int quantity,
                                          String reason, String createdBy) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        boolean wasLowStock = product.isLowStock();
        applyToStock(product, type, quantity);

        StockMovement movement = new StockMovement(
                product, type, quantity, product.getStock(), reason, createdBy);
        movements.save(movement);

        if (!wasLowStock && product.isLowStock()) {
            String message = "Stock bajo: " + product.getName()
                    + " (" + product.getStock() + "/" + product.getMinStock() + ")";
            alerts.save(new StockAlert(product, message));
        }
        // el stock del producto se persiste por dirty checking al cerrar la transaccion
        return movement;
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
