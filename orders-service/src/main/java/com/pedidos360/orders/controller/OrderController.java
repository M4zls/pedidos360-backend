package com.pedidos360.orders.controller;

import java.net.URI;
import java.util.List;

import com.pedidos360.orders.controller.dto.ChangeStatusRequest;
import com.pedidos360.orders.controller.dto.CreateOrderRequest;
import com.pedidos360.orders.controller.dto.OrderNotificationResponse;
import com.pedidos360.orders.controller.dto.OrderResponse;
import com.pedidos360.orders.controller.dto.OrderStatusChangeResponse;
import com.pedidos360.orders.controller.dto.SalesReportResponse;
import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderStatus;
import com.pedidos360.orders.service.OrderAccessDeniedException;
import com.pedidos360.orders.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del microservicio de pedidos. Rutas bajo {@code /api/orders}, todas
 * requieren sesion.
 *
 * <ul>
 *   <li>CLIENTE: crea pedidos y ve/cancela los suyos.</li>
 *   <li>ADMIN / OPERADOR: ven todos los pedidos y cambian su estado.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** Arma un pedido: CLIENTE y ADMIN. OPERADOR (cocina) no arma pedidos. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest body,
                                                Authentication auth) {
        List<OrderService.LineInput> lines = body.lines().stream()
                .map(l -> new OrderService.LineInput(l.productId(), l.quantity()))
                .toList();
        String bearer = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
        CustomerOrder created = orders.create(
                CurrentUser.email(auth), CurrentUser.displayName(auth), body.note(), lines, bearer);
        return ResponseEntity
                .created(URI.create("/api/orders/" + created.getId()))
                .body(OrderResponse.from(created));
    }

    /** Pedidos del usuario logueado: CLIENTE y ADMIN. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public List<OrderResponse> mine(Authentication auth) {
        return orders.listForCustomer(CurrentUser.email(auth)).stream()
                .map(OrderResponse::from).toList();
    }

    /** Todos los pedidos (staff). Filtro opcional por estado. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public List<OrderResponse> all(@RequestParam(required = false) OrderStatus status) {
        return orders.listAll(status).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication auth) {
        CustomerOrder order = orders.get(id);
        boolean owner = order.getCustomerEmail().equalsIgnoreCase(CurrentUser.email(auth));
        if (!owner && !CurrentUser.isStaff(auth)) {
            throw new OrderAccessDeniedException();
        }
        return OrderResponse.from(order);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public OrderResponse changeStatus(@PathVariable Long id,
                                      @Valid @RequestBody ChangeStatusRequest body,
                                      Authentication auth) {
        return OrderResponse.from(orders.changeStatus(id, body.status(), CurrentUser.email(auth)));
    }

    /** Cancela un pedido: CLIENTE (el suyo, si sigue PENDIENTE) y ADMIN (cualquiera). */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public OrderResponse cancel(@PathVariable Long id, Authentication auth) {
        return OrderResponse.from(
                orders.cancel(id, CurrentUser.email(auth), CurrentUser.isStaff(auth)));
    }

    /** Historial de cambios de estado del pedido: dueño o staff. */
    @GetMapping("/{id}/history")
    public List<OrderStatusChangeResponse> history(@PathVariable Long id, Authentication auth) {
        CustomerOrder order = orders.get(id);
        boolean owner = order.getCustomerEmail().equalsIgnoreCase(CurrentUser.email(auth));
        if (!owner && !CurrentUser.isStaff(auth)) {
            throw new OrderAccessDeniedException();
        }
        return orders.history(id).stream().map(OrderStatusChangeResponse::from).toList();
    }

    /** Notificaciones del cliente logueado sobre sus pedidos. */
    @GetMapping("/notifications/mine")
    public List<OrderNotificationResponse> myNotifications(Authentication auth) {
        return orders.notificationsFor(CurrentUser.email(auth)).stream()
                .map(OrderNotificationResponse::from).toList();
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable Long id, Authentication auth) {
        orders.markNotificationRead(id, CurrentUser.email(auth));
        return ResponseEntity.noContent().build();
    }

    /** Reporte de ventas agregado por estado. Solo ADMIN. */
    @GetMapping("/reports/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public SalesReportResponse salesReport() {
        return SalesReportResponse.from(orders.salesReport());
    }
}
