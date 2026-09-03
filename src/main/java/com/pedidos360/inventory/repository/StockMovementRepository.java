package com.pedidos360.inventory.repository;

import java.util.List;

import com.pedidos360.inventory.domain.StockMovement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Historial de movimientos de un producto, del mas reciente al mas antiguo.
     * `join fetch` trae el producto en la misma consulta: el DTO lo lee fuera de
     * la transaccion (open-in-view=false) y si no, seria LazyInitializationException.
     */
    @Query("select m from StockMovement m join fetch m.product"
            + " where m.product.id = :productId order by m.createdAt desc")
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);
}
