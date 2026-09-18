package com.pedidos360.orders.repository;

import java.util.List;

import com.pedidos360.orders.domain.OrderNotification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, Long> {

    List<OrderNotification> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);
}
