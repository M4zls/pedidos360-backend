package com.pedidos360.inventory.repository;

import java.util.List;

import com.pedidos360.inventory.domain.StockAlert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    /** `join fetch` trae el producto en la misma consulta (open-in-view=false). */
    @Query("select a from StockAlert a join fetch a.product order by a.createdAt desc")
    List<StockAlert> findAllByOrderByCreatedAtDesc();
}
