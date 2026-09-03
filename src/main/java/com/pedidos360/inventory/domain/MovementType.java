package com.pedidos360.inventory.domain;

/** Tipo de movimiento de stock. */
public enum MovementType {
    /** Entrada de mercaderia (compra, produccion). Suma stock. */
    IN,
    /** Salida de mercaderia (venta, consumo, merma). Resta stock. */
    OUT,
    /** Ajuste manual de inventario (correccion de conteo). Fija el stock al valor indicado. */
    ADJUSTMENT
}
