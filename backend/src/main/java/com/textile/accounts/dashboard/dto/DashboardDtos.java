package com.textile.accounts.dashboard.dto;

import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceResponse;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceResponse;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {

    public record DashboardKpis(
            BigDecimal todaySales,
            BigDecimal thisMonthSales,
            BigDecimal customerOutstanding,
            BigDecimal supplierOutstanding,
            BigDecimal cashBalance,
            BigDecimal bankBalance
    ) {}

    public record DashboardSummary(
            DashboardKpis kpis,
            List<SalesInvoiceResponse> recentSales,
            List<PurchaseInvoiceResponse> recentPurchases
    ) {}
}
