package com.pedidos360.orders.repository;

import java.util.List;
import java.util.Optional;

import com.pedidos360.orders.domain.CustomerOrder;
import com.pedidos360.orders.domain.OrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerOrder> findWithLinesById(Long id);

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
