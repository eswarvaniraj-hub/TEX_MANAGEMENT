package com.textile.accounts.ledger.model;

import com.textile.accounts.settings.model.FinancialYear;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_no", nullable = false, unique = true, length = 20)
    private String entryNo;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // SALES_INVOICE, PURCHASE_INVOICE, PAYMENT, EXPENSE, TRANSFER, OTHER_TXN, OPENING, MANUAL

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false, length = 255)
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_entry_id", unique = true)
    private JournalEntry reversesEntry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("lineNo ASC")
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntryNo() { return entryNo; }
    public void setEntryNo(String entryNo) { this.entryNo = entryNo; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public FinancialYear getFinancialYear() { return financialYear; }
    public void setFinancialYear(FinancialYear financialYear) { this.financialYear = financialYear; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JournalEntry getReversesEntry() { return reversesEntry; }
    public void setReversesEntry(JournalEntry reversesEntry) { this.reversesEntry = reversesEntry; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<JournalLine> getLines() { return lines; }
    public void setLines(List<JournalLine> lines) { this.lines = lines; }

    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }
}
