package com.textile.accounts.settings.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_sequence", uniqueConstraints = {
    @UniqueConstraint(name = "uq_seq_doc_type_fy", columnNames = {"doc_type", "financial_year_id"})
})
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(nullable = false, length = 10)
    private String prefix;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber = 1L;

    public DocumentSequence() {}

    public DocumentSequence(String docType, FinancialYear financialYear, String prefix, Long nextNumber) {
        this.docType = docType;
        this.financialYear = financialYear;
        this.prefix = prefix;
        this.nextNumber = nextNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public FinancialYear getFinancialYear() { return financialYear; }
    public void setFinancialYear(FinancialYear financialYear) { this.financialYear = financialYear; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public Long getNextNumber() { return nextNumber; }
    public void setNextNumber(Long nextNumber) { this.nextNumber = nextNumber; }
}
