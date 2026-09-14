package com.pedidos360.orders.service;

import java.util.List;

import com.pedidos360.orders.client.InventoryClient;
import com.pedidos360.orders.client.ProductView;
import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderLine;
import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Reglas de negocio de los pedidos. CRUD basico + cambios de estado. No toca
 * el stock del inventario: le pide el producto al servicio de inventario por
 * HTTP ({@link InventoryClient}) solo para validar y congelar el precio.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final InventoryClient inventory;

    public OrderService(OrderRepository orders, InventoryClient inventory) {
        this.orders = orders;
        this.inventory = inventory;
    }

    public record LineInput(Long productId, int quantity) {}

    public CustomerOrder create(String customerEmail, String customerName, String note,
                                List<LineInput> lines, String bearerToken) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("El pedido no tiene lineas");
        }
        CustomerOrder order = new CustomerOrder(customerEmail, customerName, trimToNull(note));
        for (LineInput line : lines) {
            if (line.quantity() <= 0) {
                throw new InvalidOrderException("La cantidad debe ser mayor a 0");
            }
            ProductView product = inventory.findProduct(line.productId(), bearerToken)
                    .orElseThrow(() -> new InvalidOrderException("El producto " + line.productId() + " no existe"));
            if (!product.active()) {
                throw new InvalidOrderException("El producto " + product.name() + " no esta disponible");
            }
            order.addLine(new OrderLine(
                    product.id(), product.sku(), product.name(),
                    product.unitPrice(), line.quantity()));
        }
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> listForCustomer(String customerEmail) {
        return orders.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(customerEmail);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> listAll(OrderStatus status) {
        return status != null
                ? orders.findByStatusOrderByCreatedAtDesc(status)
                : orders.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(Long id) {
        return orders.findWithLinesById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Solo staff. Cualquier estado no terminal -> cualquier otro. */
    public CustomerOrder changeStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order = get(id);
        if (order.getStatus().isTerminal()) {
            throw new InvalidOrderException(
                    "El pedido esta " + order.getStatus() + " y no se puede modificar");
        }
        if (newStatus == OrderStatus.PENDIENTE) {
            throw new InvalidOrderException("No se puede volver a PENDIENTE");
        }
        order.setStatus(newStatus);
        return order;
    }

    /**
     * Cancela un pedido. El staff puede cancelar cualquier pedido no terminal;
     * el cliente dueño solo si sigue PENDIENTE.
     */
    public CustomerOrder cancel(Long id, String requesterEmail, boolean staff) {
        CustomerOrder order = get(id);
        boolean owner = order.getCustomerEmail().equalsIgnoreCase(requesterEmail);
        if (!staff && !owner) {
            throw new OrderAccessDeniedException();
        }
        if (order.getStatus().isTerminal()) {
            throw new InvalidOrderException("El pedido ya esta " + order.getStatus());
        }
        if (!staff && order.getStatus() != OrderStatus.PENDIENTE) {
            throw new InvalidOrderException("Solo se puede cancelar mientras esta PENDIENTE");
        }
        order.setStatus(OrderStatus.CANCELADO);
        return order;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
