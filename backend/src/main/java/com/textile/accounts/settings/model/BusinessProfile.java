package com.textile.accounts.settings.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "business_profile")
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_name", nullable = false, length = 150)
    private String legalName;

    @Column(name = "trade_name", nullable = false, length = 150)
    private String tradeName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "invoice_footer", columnDefinition = "TEXT")
    private String invoiceFooter;

    @Column(name = "bank_details_text", columnDefinition = "TEXT")
    private String bankDetailsText;

    @Column(name = "gst_registered", nullable = false)
    private boolean gstRegistered = true;

    @Column(name = "round_invoice_total", nullable = false)
    private boolean roundInvoiceTotal = true;

    @Column(name = "lock_date")
    private LocalDate lockDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version = 0L;

    public BusinessProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInvoiceFooter() { return invoiceFooter; }
    public void setInvoiceFooter(String invoiceFooter) { this.invoiceFooter = invoiceFooter; }

    public String getBankDetailsText() { return bankDetailsText; }
    public void setBankDetailsText(String bankDetailsText) { this.bankDetailsText = bankDetailsText; }

    public boolean isGstRegistered() { return gstRegistered; }
    public void setGstRegistered(boolean gstRegistered) { this.gstRegistered = gstRegistered; }

    public boolean isRoundInvoiceTotal() { return roundInvoiceTotal; }
    public void setRoundInvoiceTotal(boolean roundInvoiceTotal) { this.roundInvoiceTotal = roundInvoiceTotal; }

    public LocalDate getLockDate() { return lockDate; }
    public void setLockDate(LocalDate lockDate) { this.lockDate = lockDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
