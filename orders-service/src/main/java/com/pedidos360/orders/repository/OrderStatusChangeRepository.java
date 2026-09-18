package com.pedidos360.orders.repository;

import java.util.List;

import com.pedidos360.orders.domain.OrderStatusChange;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderStatusChangeRepository extends JpaRepository<OrderStatusChange, Long> {

    /** `join fetch` trae el pedido en la misma consulta (open-in-view=false). */
    @Query("select c from OrderStatusChange c join fetch c.order"
            + " where c.order.id = :orderId order by c.changedAt desc")
    List<OrderStatusChange> findByOrderIdOrderByChangedAtDesc(@Param("orderId") Long orderId);
}
