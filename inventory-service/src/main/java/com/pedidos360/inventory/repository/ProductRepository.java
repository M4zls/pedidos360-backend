package com.pedidos360.inventory.repository;

import java.util.List;
import java.util.Optional;

import com.pedidos360.inventory.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Capa de repositorio del inventario (Spring Data JPA). Los metodos derivados
 * del nombre y las @Query cubren las consultas que necesita el servicio; el
 * CRUD basico lo aporta {@link JpaRepository}.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findByActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(String category);

    /** Productos activos cuyo stock esta en o por debajo de su umbral de reposicion. */
    @Query("select p from Product p where p.active = true and p.stock <= p.minStock order by p.stock asc")
    List<Product> findLowStock();
}
