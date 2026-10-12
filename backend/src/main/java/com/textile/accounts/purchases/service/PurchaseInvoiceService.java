package com.textile.accounts.purchases.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.service.PostingRequest;
import com.textile.accounts.ledger.service.PostingRequest.PostingLineRequest;
import com.textile.accounts.ledger.service.PostingService;
import com.textile.accounts.masterdata.model.Product;
import com.textile.accounts.masterdata.model.Supplier;
import com.textile.accounts.masterdata.model.Unit;
import com.textile.accounts.masterdata.repository.ProductRepository;
import com.textile.accounts.masterdata.repository.SupplierRepository;
import com.textile.accounts.masterdata.repository.UnitRepository;
import com.textile.accounts.purchases.dto.PurchaseDtos.CalculatedPurchaseLineResponse;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseCalculationResponse;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceLineRequest;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceRequest;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceResponse;
import com.textile.accounts.purchases.dto.PurchaseDtos.ReversePurchaseRequest;
import com.textile.accounts.purchases.model.PurchaseInvoice;
import com.textile.accounts.purchases.model.PurchaseInvoiceLine;
import com.textile.accounts.purchases.repository.PurchaseInvoiceRepository;
import com.textile.accounts.settings.model.BusinessProfile;
import com.textile.accounts.settings.model.FinancialYear;
import com.textile.accounts.settings.repository.FinancialYearRepository;
import com.textile.accounts.settings.service.DocumentNumberService;
import com.textile.accounts.settings.service.SettingsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PurchaseInvoiceService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final FinancialYearRepository financialYearRepository;
    private final AccountRepository accountRepository;
    private final SettingsService settingsService;
    private final PostingService postingService;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public PurchaseInvoiceService(
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            UnitRepository unitRepository,
            FinancialYearRepository financialYearRepository,
            AccountRepository accountRepository,
            SettingsService settingsService,
            PostingService postingService,
            DocumentNumberService documentNumberService,
            AuditService auditService
    ) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.financialYearRepository = financialYearRepository;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
        this.postingService = postingService;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    public static String normalizeInvoiceNo(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    @Transactional(readOnly = true)
    public Page<PurchaseInvoiceResponse> search(Long supplierId, String status, LocalDate from, LocalDate to, String q, Pageable pageable) {
        return purchaseInvoiceRepository.searchPurchaseInvoices(supplierId, status, from, to, q, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseInvoiceResponse getById(Long id) {
        return purchaseInvoiceRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found: " + id));
    }

    public PurchaseCalculationResponse calculate(PurchaseInvoiceRequest request) {
        BusinessProfile profile = settingsService.getProfileEntity();
        String buyerStateCode = profile.getStateCode();

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));

        boolean isInterstate = !buyerStateCode.equalsIgnoreCase(supplier.getState().getCode());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;

        List<CalculatedPurchaseLineResponse> lines = new ArrayList<>();
        int lineNo = 1;

        for (PurchaseInvoiceLineRequest reqLine : request.lines()) {
            Product product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + reqLine.productId()));

            Unit unit = unitRepository.findById(reqLine.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + reqLine.unitId()));

            BigDecimal qty = reqLine.quantity().setScale(unit.getDecimalPlaces(), RoundingMode.HALF_UP);
            BigDecimal rate = reqLine.rate().setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineAmount = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal discount = reqLine.discountAmount() != null
                    ? reqLine.discountAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);

            BigDecimal taxable = lineAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);

            BigDecimal taxRate = reqLine.taxRatePercent() != null
                    ? reqLine.taxRatePercent()
                    : (product.getDefaultTaxRate() != null ? product.getDefaultTaxRate().getRatePercent() : BigDecimal.ZERO);

            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);

            if (isInterstate) {
                igst = taxable.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                BigDecimal halfRate = taxRate.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                cgst = taxable.multiply(halfRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                sgst = taxable.multiply(halfRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }

            BigDecimal lineTotal = taxable.add(cgst).add(sgst).add(igst);

            subtotal = subtotal.add(lineAmount);
            discountTotal = discountTotal.add(discount);
            taxableTotal = taxableTotal.add(taxable);
            cgstTotal = cgstTotal.add(cgst);
            sgstTotal = sgstTotal.add(sgst);
            igstTotal = igstTotal.add(igst);

            lines.add(new CalculatedPurchaseLineResponse(
                    lineNo++,
                    product.getId(),
                    product.getName(),
                    reqLine.description() != null ? reqLine.description() : product.getDescription(),
                    reqLine.hsnCode() != null ? reqLine.hsnCode() : product.getHsnCode(),
                    qty,
                    unit.getId(),
                    unit.getCode(),
                    rate,
                    lineAmount,
                    discount,
                    taxable,
                    taxRate,
                    cgst,
                    sgst,
                    igst,
                    lineTotal
            ));
        }

        BigDecimal rawGrandTotal = taxableTotal.add(cgstTotal).add(sgstTotal).add(igstTotal);
        BigDecimal roundOff = BigDecimal.ZERO.setScale(2);
        BigDecimal grandTotal = rawGrandTotal;

        if (profile.isRoundInvoiceTotal()) {
            BigDecimal rounded = rawGrandTotal.setScale(0, RoundingMode.HALF_UP).setScale(2);
            roundOff = rounded.subtract(rawGrandTotal);
            grandTotal = rounded;
        }

        return new PurchaseCalculationResponse(
                isInterstate,
                subtotal,
                discountTotal,
                taxableTotal,
                cgstTotal,
                sgstTotal,
                igstTotal,
                roundOff,
                grandTotal,
                lines
        );
    }

    @Transactional
    public PurchaseInvoiceResponse createDraft(PurchaseInvoiceRequest request, String actor) {
        postingService.validateLockDate(request.purchaseDate());

        FinancialYear fy = financialYearRepository.findByDate(request.purchaseDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year configured for: " + request.purchaseDate()));

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));

        String normalizedNo = normalizeInvoiceNo(request.supplierInvoiceNo());
        if (purchaseInvoiceRepository.existsDuplicateSupplierInvoice(supplier.getId(), normalizedNo, null)) {
            throw new IllegalArgumentException("DUPLICATE_SUPPLIER_INVOICE: Invoice number '" + request.supplierInvoiceNo() + "' is already recorded for supplier " + supplier.getName());
        }

        PurchaseCalculationResponse calc = calculate(request);

        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setFinancialYear(fy);
        invoice.setSupplier(supplier);
        invoice.setSupplierInvoiceNo(request.supplierInvoiceNo().trim());
        invoice.setSupplierInvoiceNoNormalized(normalizedNo);
        invoice.setSupplierInvoiceDate(request.supplierInvoiceDate());
        invoice.setPurchaseDate(request.purchaseDate());
        invoice.setSupplierNameSnapshot(supplier.getName());
        invoice.setSupplierGstinSnapshot(supplier.getGstin());
        invoice.setSupplierAddressSnapshot(supplier.getAddress());
        invoice.setStatus("DRAFT");
        invoice.setSubtotal(calc.subtotal());
        invoice.setDiscountTotal(calc.discountTotal());
        invoice.setTaxableTotal(calc.taxableTotal());
        invoice.setCgstTotal(calc.cgstTotal());
        invoice.setSgstTotal(calc.sgstTotal());
        invoice.setIgstTotal(calc.igstTotal());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setNotes(request.notes());
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        mapLineItems(invoice, calc.lines());

        PurchaseInvoice saved = purchaseInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE_DRAFT",
                "PURCHASE_INVOICE",
                saved.getId().toString(),
                "Created draft purchase invoice for: " + supplier.getName() + " (#" + saved.getSupplierInvoiceNo() + ")",
                Map.of("supplierId", supplier.getId(), "grandTotal", saved.getGrandTotal())
        );

        return toResponse(saved);
    }

    @Transactional
    public PurchaseInvoiceResponse updateDraft(Long id, PurchaseInvoiceRequest request, Long submittedVersion, String actor) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found: " + id));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only DRAFT purchase invoices can be edited.");
        }

        if (submittedVersion != null && !Objects.equals(invoice.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Invoice was modified by another transaction.");
        }

        postingService.validateLockDate(request.purchaseDate());

        FinancialYear fy = financialYearRepository.findByDate(request.purchaseDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year configured for: " + request.purchaseDate()));

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));

        String normalizedNo = normalizeInvoiceNo(request.supplierInvoiceNo());
        if (purchaseInvoiceRepository.existsDuplicateSupplierInvoice(supplier.getId(), normalizedNo, invoice.getId())) {
            throw new IllegalArgumentException("DUPLICATE_SUPPLIER_INVOICE: Invoice number '" + request.supplierInvoiceNo() + "' is already recorded for supplier " + supplier.getName());
        }

        PurchaseCalculationResponse calc = calculate(request);

        invoice.setFinancialYear(fy);
        invoice.setSupplier(supplier);
        invoice.setSupplierInvoiceNo(request.supplierInvoiceNo().trim());
        invoice.setSupplierInvoiceNoNormalized(normalizedNo);
        invoice.setSupplierInvoiceDate(request.supplierInvoiceDate());
        invoice.setPurchaseDate(request.purchaseDate());
        invoice.setSupplierNameSnapshot(supplier.getName());
        invoice.setSupplierGstinSnapshot(supplier.getGstin());
        invoice.setSupplierAddressSnapshot(supplier.getAddress());
        invoice.setSubtotal(calc.subtotal());
        invoice.setDiscountTotal(calc.discountTotal());
        invoice.setTaxableTotal(calc.taxableTotal());
        invoice.setCgstTotal(calc.cgstTotal());
        invoice.setSgstTotal(calc.sgstTotal());
        invoice.setIgstTotal(calc.igstTotal());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setNotes(request.notes());
        invoice.setUpdatedAt(Instant.now());

        invoice.getLines().clear();
        mapLineItems(invoice, calc.lines());

        PurchaseInvoice updated = purchaseInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE_DRAFT",
                "PURCHASE_INVOICE",
                updated.getId().toString(),
                "Updated draft purchase invoice for: " + supplier.getName(),
                Map.of("supplierId", supplier.getId(), "grandTotal", updated.getGrandTotal())
        );

        return toResponse(updated);
    }

    @Transactional
    public PurchaseInvoiceResponse postInvoice(Long id, String actor) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found: " + id));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Purchase invoice is already " + invoice.getStatus());
        }

        postingService.validateLockDate(invoice.getPurchaseDate());

        // Allocate gapless sequential voucher number
        String voucherNo = documentNumberService.getNextDocumentNumber("PUR", invoice.getPurchaseDate());
        invoice.setVoucherNo(voucherNo);

        // Fetch Accounts for double entry (Recipe P1)
        Account apAccount = accountRepository.findBySystemRole("AP_CONTROL")
                .orElseThrow(() -> new IllegalStateException("Missing AP_CONTROL account"));
        Account purchaseAccount = accountRepository.findBySystemRole("PURCHASES_YARN")
                .orElseThrow(() -> new IllegalStateException("Missing PURCHASES_YARN account"));

        List<PostingLineRequest> postingLines = new ArrayList<>();

        // 1. Credit Supplier Payables (AP_CONTROL)
        postingLines.add(PostingLineRequest.credit(
                apAccount,
                invoice.getGrandTotal(),
                null,
                invoice.getSupplier(),
                "Purchase Voucher " + voucherNo + " (Inv #" + invoice.getSupplierInvoiceNo() + ")"
        ));

        // 2. Debit Purchase Expense
        postingLines.add(PostingLineRequest.debit(
                purchaseAccount,
                invoice.getTaxableTotal(),
                null,
                null,
                "Material Purchases - " + voucherNo
        ));

        // 3. Debit Input GST
        if (invoice.getCgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account cgstAccount = accountRepository.findBySystemRole("GST_INPUT_CGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_INPUT_CGST account"));
            postingLines.add(PostingLineRequest.debit(cgstAccount, invoice.getCgstTotal(), null, null, "CGST Input"));
        }
        if (invoice.getSgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account sgstAccount = accountRepository.findBySystemRole("GST_INPUT_SGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_INPUT_SGST account"));
            postingLines.add(PostingLineRequest.debit(sgstAccount, invoice.getSgstTotal(), null, null, "SGST Input"));
        }
        if (invoice.getIgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account igstAccount = accountRepository.findBySystemRole("GST_INPUT_IGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_INPUT_IGST account"));
            postingLines.add(PostingLineRequest.debit(igstAccount, invoice.getIgstTotal(), null, null, "IGST Input"));
        }

        // 4. Round off adjustment
        if (invoice.getRoundOff().compareTo(BigDecimal.ZERO) != 0) {
            Account roundOffAccount = accountRepository.findBySystemRole("ROUND_OFF")
                    .orElseThrow(() -> new IllegalStateException("Missing ROUND_OFF account"));
            if (invoice.getRoundOff().compareTo(BigDecimal.ZERO) > 0) {
                // Round off added -> debit round off
                postingLines.add(PostingLineRequest.debit(roundOffAccount, invoice.getRoundOff(), null, null, "Round off"));
            } else {
                // Round off deducted -> credit round off
                postingLines.add(PostingLineRequest.credit(roundOffAccount, invoice.getRoundOff().abs(), null, null, "Round off"));
            }
        }

        PostingRequest postingRequest = new PostingRequest(
                invoice.getPurchaseDate(),
                "PURCHASE_INVOICE",
                invoice.getId(),
                "Purchase: " + voucherNo + " from " + invoice.getSupplierNameSnapshot() + " (Inv #" + invoice.getSupplierInvoiceNo() + ")",
                actor != null ? actor : "system",
                postingLines
        );

        JournalEntry journalEntry = postingService.post(postingRequest);

        invoice.setJournalEntry(journalEntry);
        invoice.setStatus("POSTED");
        invoice.setPostedAt(Instant.now());
        invoice.setPostedBy(actor != null ? actor : "system");
        invoice.setUpdatedAt(Instant.now());

        PurchaseInvoice saved = purchaseInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "POST_PURCHASE",
                "PURCHASE_INVOICE",
                saved.getId().toString(),
                "Posted Purchase: " + voucherNo + " (JV: " + journalEntry.getEntryNo() + ")",
                Map.of("voucherNo", voucherNo, "grandTotal", saved.getGrandTotal())
        );

        return toResponse(saved);
    }

    @Transactional
    public PurchaseInvoiceResponse reverseInvoice(Long id, ReversePurchaseRequest request, String actor) {
        PurchaseInvoice invoice = purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase invoice not found: " + id));

        if (!"POSTED".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only POSTED invoices can be reversed.");
        }

        if (invoice.getJournalEntry() == null) {
            throw new IllegalStateException("Invoice has no associated journal entry.");
        }

        LocalDate reversalDate = LocalDate.now();
        postingService.validateLockDate(reversalDate);

        JournalEntry reversalEntry = postingService.reverseEntry(
                invoice.getJournalEntry().getId(),
                reversalDate,
                request != null ? request.reason() : "Purchase cancellation",
                actor
        );

        invoice.setStatus("REVERSED");
        invoice.setReversalEntry(reversalEntry);
        invoice.setReversedAt(Instant.now());
        invoice.setReversalReason(request != null ? request.reason() : "Cancelled");
        invoice.setUpdatedAt(Instant.now());

        PurchaseInvoice saved = purchaseInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "REVERSE_PURCHASE",
                "PURCHASE_INVOICE",
                saved.getId().toString(),
                "Reversed Purchase Invoice: " + saved.getVoucherNo() + " (Reversal JV: " + reversalEntry.getEntryNo() + ")",
                Map.of("voucherNo", saved.getVoucherNo(), "reason", saved.getReversalReason())
        );

        return toResponse(saved);
    }

    private void mapLineItems(PurchaseInvoice invoice, List<CalculatedPurchaseLineResponse> calcLines) {
        for (CalculatedPurchaseLineResponse cl : calcLines) {
            Product product = productRepository.getReferenceById(cl.productId());
            Unit unit = unitRepository.getReferenceById(cl.unitId());

            PurchaseInvoiceLine line = new PurchaseInvoiceLine();
            line.setLineNo(cl.lineNo());
            line.setProduct(product);
            line.setDescription(cl.description());
            line.setHsnCode(cl.hsnCode());
            line.setQuantity(cl.quantity());
            line.setUnit(unit);
            line.setRate(cl.rate());
            line.setLineAmount(cl.lineAmount());
            line.setDiscountAmount(cl.discountAmount());
            line.setTaxableValue(cl.taxableValue());
            line.setTaxRatePercent(cl.taxRatePercent());
            line.setCgst(cl.cgst());
            line.setSgst(cl.sgst());
            line.setIgst(cl.igst());
            line.setLineTotal(cl.lineTotal());

            invoice.addLine(line);
        }
    }

    public PurchaseInvoiceResponse toResponse(PurchaseInvoice pi) {
        List<CalculatedPurchaseLineResponse> lines = pi.getLines().stream().map(l -> new CalculatedPurchaseLineResponse(
                l.getLineNo(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getDescription(),
                l.getHsnCode(),
                l.getQuantity(),
                l.getUnit().getId(),
                l.getUnit().getCode(),
                l.getRate(),
                l.getLineAmount(),
                l.getDiscountAmount(),
                l.getTaxableValue(),
                l.getTaxRatePercent(),
                l.getCgst(),
                l.getSgst(),
                l.getIgst(),
                l.getLineTotal()
        )).toList();

        return new PurchaseInvoiceResponse(
                pi.getId(),
                pi.getVoucherNo(),
                pi.getSupplier().getId(),
                pi.getSupplierNameSnapshot(),
                pi.getSupplierGstinSnapshot(),
                pi.getSupplierAddressSnapshot(),
                pi.getSupplierInvoiceNo(),
                pi.getSupplierInvoiceDate(),
                pi.getPurchaseDate(),
                pi.getStatus(),
                pi.getSubtotal(),
                pi.getDiscountTotal(),
                pi.getTaxableTotal(),
                pi.getCgstTotal(),
                pi.getSgstTotal(),
                pi.getIgstTotal(),
                pi.getRoundOff(),
                pi.getGrandTotal(),
                pi.getNotes(),
                pi.getJournalEntry() != null ? pi.getJournalEntry().getId() : null,
                pi.getReversalEntry() != null ? pi.getReversalEntry().getId() : null,
                pi.getPostedAt(),
                pi.getPostedBy(),
                pi.getReversedAt(),
                pi.getReversalReason(),
                pi.getVersion(),
                lines
        );
    }
}
