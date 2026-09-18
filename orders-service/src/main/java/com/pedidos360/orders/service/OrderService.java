package com.pedidos360.orders.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.pedidos360.orders.client.InventoryClient;
import com.pedidos360.orders.client.ProductView;
import com.pedidos360.orders.controller.dto.StatusCount;
import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderLine;
import com.pedidos360.orders.domain.OrderNotification;
import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.domain.OrderStatusChange;
import com.pedidos360.orders.repository.OrderNotificationRepository;
import com.pedidos360.orders.repository.OrderRepository;
import com.pedidos360.orders.repository.OrderStatusChangeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Reglas de negocio de los pedidos. CRUD basico + cambios de estado. No toca
 * el stock del inventario: le pide el producto al servicio de inventario por
 * HTTP ({@link InventoryClient}) solo para validar y congelar el precio. Cada
 * cambio de estado deja un {@link OrderStatusChange} (historial) y, en los
 * hitos que le importan al cliente, una {@link OrderNotification}.
 */
@Service
@Transactional
public class OrderService {

    /** Hitos que se le notifican al cliente (los intermedios no le aportan). */
    private static final Map<OrderStatus, String> NOTIFIABLE_LABELS = new EnumMap<>(OrderStatus.class);
    static {
        NOTIFIABLE_LABELS.put(OrderStatus.LISTO, "listo");
        NOTIFIABLE_LABELS.put(OrderStatus.DESPACHADO, "despachado");
        NOTIFIABLE_LABELS.put(OrderStatus.ENTREGADO, "entregado");
        NOTIFIABLE_LABELS.put(OrderStatus.CANCELADO, "cancelado");
    }

    private final OrderRepository orders;
    private final InventoryClient inventory;
    private final OrderStatusChangeRepository statusChanges;
    private final OrderNotificationRepository notifications;

    public OrderService(OrderRepository orders, InventoryClient inventory,
                        OrderStatusChangeRepository statusChanges,
                        OrderNotificationRepository notifications) {
        this.orders = orders;
        this.inventory = inventory;
        this.statusChanges = statusChanges;
        this.notifications = notifications;
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
    public CustomerOrder changeStatus(Long id, OrderStatus newStatus, String changedBy) {
        CustomerOrder order = get(id);
        if (order.getStatus().isTerminal()) {
            throw new InvalidOrderException(
                    "El pedido esta " + order.getStatus() + " y no se puede modificar");
        }
        if (newStatus == OrderStatus.PENDIENTE) {
            throw new InvalidOrderException("No se puede volver a PENDIENTE");
        }
        applyStatusChange(order, newStatus, changedBy);
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
        applyStatusChange(order, OrderStatus.CANCELADO, requesterEmail);
        return order;
    }

    /** Muta el estado y deja historial + notificacion, todo en la misma transaccion. */
    private void applyStatusChange(CustomerOrder order, OrderStatus newStatus, String changedBy) {
        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        statusChanges.save(new OrderStatusChange(order, previous, newStatus, changedBy));

        String label = NOTIFIABLE_LABELS.get(newStatus);
        if (label != null) {
            String message = "Tu pedido #" + order.getId() + " está " + label;
            notifications.save(new OrderNotification(order.getCustomerEmail(), order.getId(), message));
        }
    }

    @Transactional(readOnly = true)
    public List<OrderStatusChange> history(Long id) {
        if (!orders.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        return statusChanges.findByOrderIdOrderByChangedAtDesc(id);
    }

    @Transactional(readOnly = true)
    public List<OrderNotification> notificationsFor(String customerEmail) {
        return notifications.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(customerEmail);
    }

    /** Solo el dueño puede marcar su notificacion como leida. */
    public void markNotificationRead(Long notificationId, String requesterEmail) {
        OrderNotification notification = notifications.findById(notificationId)
                .orElseThrow(OrderAccessDeniedException::new);
        if (!notification.getCustomerEmail().equalsIgnoreCase(requesterEmail)) {
            throw new OrderAccessDeniedException();
        }
        notification.markRead();
    }

    @Transactional(readOnly = true)
    public List<StatusCount> salesReport() {
        return orders.countAndTotalByStatus();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
