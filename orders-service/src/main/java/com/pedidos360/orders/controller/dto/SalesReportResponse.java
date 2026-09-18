package com.pedidos360.orders.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import com.pedidos360.orders.domain.OrderStatus;

/** Reporte de ventas: totales agregados por estado del pedido. */
public record SalesReportResponse(
        BigDecimal totalRevenue,
        long deliveredCount,
        long activeCount,
        long cancelledCount,
        List<StatusCount> byStatus
) {
    public static SalesReportResponse from(List<StatusCount> byStatus) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long deliveredCount = 0;
        long activeCount = 0;
        long cancelledCount = 0;

        for (StatusCount row : byStatus) {
            if (row.status() == OrderStatus.ENTREGADO) {
                totalRevenue = totalRevenue.add(row.total());
                deliveredCount = row.count();
            } else if (row.status() == OrderStatus.CANCELADO) {
                cancelledCount = row.count();
            } else {
                activeCount += row.count();
            }
        }
        return new SalesReportResponse(totalRevenue, deliveredCount, activeCount, cancelledCount, byStatus);
    }
}
