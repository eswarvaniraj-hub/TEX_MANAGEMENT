package com.textile.accounts.dashboard.service;

import com.textile.accounts.banking.model.PaymentAccount;
import com.textile.accounts.banking.repository.PaymentAccountRepository;
import com.textile.accounts.dashboard.dto.DashboardDtos.DashboardKpis;
import com.textile.accounts.dashboard.dto.DashboardDtos.DashboardSummary;
import com.textile.accounts.ledger.repository.JournalLineRepository;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceResponse;
import com.textile.accounts.purchases.repository.PurchaseInvoiceRepository;
import com.textile.accounts.purchases.service.PurchaseInvoiceService;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceResponse;
import com.textile.accounts.sales.repository.SalesInvoiceRepository;
import com.textile.accounts.sales.service.SalesInvoiceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final JournalLineRepository journalLineRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final SalesInvoiceService salesInvoiceService;
    private final PurchaseInvoiceService purchaseInvoiceService;

    public DashboardService(
            SalesInvoiceRepository salesInvoiceRepository,
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            JournalLineRepository journalLineRepository,
            PaymentAccountRepository paymentAccountRepository,
            SalesInvoiceService salesInvoiceService,
            PurchaseInvoiceService purchaseInvoiceService
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.journalLineRepository = journalLineRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.salesInvoiceService = salesInvoiceService;
        this.purchaseInvoiceService = purchaseInvoiceService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        BigDecimal todaySales = salesInvoiceRepository.getTodaySales(today);
        if (todaySales == null) todaySales = BigDecimal.ZERO;

        BigDecimal thisMonthSales = salesInvoiceRepository.getTotalSalesForPeriod(firstOfMonth, today);
        if (thisMonthSales == null) thisMonthSales = BigDecimal.ZERO;

        BigDecimal customerOutstanding = journalLineRepository.getTotalReceivablesDebit();
        if (customerOutstanding == null) customerOutstanding = BigDecimal.ZERO;

        BigDecimal supplierOutstanding = journalLineRepository.getTotalPayablesCredit();
        if (supplierOutstanding == null) supplierOutstanding = BigDecimal.ZERO;

        // Cash & Bank balances
        List<PaymentAccount> paymentAccounts = paymentAccountRepository.findByActiveTrue();
        BigDecimal cashBalance = BigDecimal.ZERO;
        BigDecimal bankBalance = BigDecimal.ZERO;

        for (PaymentAccount pa : paymentAccounts) {
            if (pa.getAccount() != null) {
                BigDecimal bal = journalLineRepository.getNetDebitBalance(pa.getAccount().getId(), null);
                if (bal == null) bal = BigDecimal.ZERO;
                if ("CASH".equalsIgnoreCase(pa.getKind())) {
                    cashBalance = cashBalance.add(bal);
                } else {
                    bankBalance = bankBalance.add(bal);
                }
            }
        }

        DashboardKpis kpis = new DashboardKpis(
                todaySales,
                thisMonthSales,
                customerOutstanding,
                supplierOutstanding,
                cashBalance,
                bankBalance
        );

        List<SalesInvoiceResponse> recentSales = salesInvoiceRepository.searchSalesInvoices(null, null, null, null, null, PageRequest.of(0, 5))
                .getContent().stream()
                .map(salesInvoiceService::toResponse)
                .toList();

        List<PurchaseInvoiceResponse> recentPurchases = purchaseInvoiceRepository.searchPurchaseInvoices(null, null, null, null, null, PageRequest.of(0, 5))
                .getContent().stream()
                .map(purchaseInvoiceService::toResponse)
                .toList();

        return new DashboardSummary(kpis, recentSales, recentPurchases);
    }
}
