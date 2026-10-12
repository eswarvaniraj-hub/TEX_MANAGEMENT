package com.textile.accounts.sales.model;

import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.settings.model.FinancialYear;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales_invoice")
public class SalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", length = 20)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "place_of_supply_state_code", nullable = false)
    private GstState placeOfSupplyState;

    @Column(name = "is_interstate", nullable = false)
    private boolean isInterstate = false;

    @Column(name = "customer_name_snapshot", nullable = false, length = 150)
    private String customerNameSnapshot;

    @Column(name = "customer_gstin_snapshot", length = 15)
    private String customerGstinSnapshot;

    @Column(name = "customer_address_snapshot", columnDefinition = "TEXT")
    private String customerAddressSnapshot;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, POSTED, REVERSED

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "taxable_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableTotal = BigDecimal.ZERO;

    @Column(name = "cgst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstTotal = BigDecimal.ZERO;

    @Column(name = "sgst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstTotal = BigDecimal.ZERO;

    @Column(name = "igst_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal igstTotal = BigDecimal.ZERO;

    @Column(name = "round_off", nullable = false, precision = 14, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_entry_id")
    private JournalEntry reversalEntry;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason")
    private String reversalReason;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by", length = 50)
    private String postedBy;

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineNo ASC")
    private List<SalesInvoiceLine> lines = new ArrayList<>();

    public SalesInvoice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public FinancialYear getFinancialYear() { return financialYear; }
    public void setFinancialYear(FinancialYear financialYear) { this.financialYear = financialYear; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public GstState getPlaceOfSupplyState() { return placeOfSupplyState; }
    public void setPlaceOfSupplyState(GstState placeOfSupplyState) { this.placeOfSupplyState = placeOfSupplyState; }

    public boolean isInterstate() { return isInterstate; }
    public void setInterstate(boolean interstate) { isInterstate = interstate; }

    public String getCustomerNameSnapshot() { return customerNameSnapshot; }
    public void setCustomerNameSnapshot(String customerNameSnapshot) { this.customerNameSnapshot = customerNameSnapshot; }

    public String getCustomerGstinSnapshot() { return customerGstinSnapshot; }
    public void setCustomerGstinSnapshot(String customerGstinSnapshot) { this.customerGstinSnapshot = customerGstinSnapshot; }

    public String getCustomerAddressSnapshot() { return customerAddressSnapshot; }
    public void setCustomerAddressSnapshot(String customerAddressSnapshot) { this.customerAddressSnapshot = customerAddressSnapshot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }

    public BigDecimal getTaxableTotal() { return taxableTotal; }
    public void setTaxableTotal(BigDecimal taxableTotal) { this.taxableTotal = taxableTotal; }

    public BigDecimal getCgstTotal() { return cgstTotal; }
    public void setCgstTotal(BigDecimal cgstTotal) { this.cgstTotal = cgstTotal; }

    public BigDecimal getSgstTotal() { return sgstTotal; }
    public void setSgstTotal(BigDecimal sgstTotal) { this.sgstTotal = sgstTotal; }

    public BigDecimal getIgstTotal() { return igstTotal; }
    public void setIgstTotal(BigDecimal igstTotal) { this.igstTotal = igstTotal; }

    public BigDecimal getRoundOff() { return roundOff; }
    public void setRoundOff(BigDecimal roundOff) { this.roundOff = roundOff; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }

    public JournalEntry getReversalEntry() { return reversalEntry; }
    public void setReversalEntry(JournalEntry reversalEntry) { this.reversalEntry = reversalEntry; }

    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }

    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }

    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<SalesInvoiceLine> getLines() { return lines; }
    public void setLines(List<SalesInvoiceLine> lines) { this.lines = lines; }

    public void addLine(SalesInvoiceLine line) {
        lines.add(line);
        line.setSalesInvoice(this);
    }
}
