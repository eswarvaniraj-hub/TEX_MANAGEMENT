package com.textile.accounts.masterdata.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType; // YARN, FABRIC, MATERIAL, SERVICE, OTHER

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_tax_rate_id")
    private TaxRate defaultTaxRate;

    @Column(name = "default_sale_rate", nullable = false, precision = 14, scale = 4)
    private BigDecimal defaultSaleRate = BigDecimal.ZERO;

    @Column(name = "default_purchase_rate", nullable = false, precision = 14, scale = 4)
    private BigDecimal defaultPurchaseRate = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public TaxRate getDefaultTaxRate() { return defaultTaxRate; }
    public void setDefaultTaxRate(TaxRate defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }

    public BigDecimal getDefaultSaleRate() { return defaultSaleRate; }
    public void setDefaultSaleRate(BigDecimal defaultSaleRate) { this.defaultSaleRate = defaultSaleRate; }

    public BigDecimal getDefaultPurchaseRate() { return defaultPurchaseRate; }
    public void setDefaultPurchaseRate(BigDecimal defaultPurchaseRate) { this.defaultPurchaseRate = defaultPurchaseRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
