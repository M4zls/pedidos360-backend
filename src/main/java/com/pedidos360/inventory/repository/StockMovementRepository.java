package com.pedidos360.inventory.repository;

import java.util.List;

import com.pedidos360.inventory.domain.StockMovement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** Historial de movimientos de un producto, del mas reciente al mas antiguo. */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
}
