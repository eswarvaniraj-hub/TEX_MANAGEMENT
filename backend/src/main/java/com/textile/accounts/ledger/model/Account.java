package com.textile.accounts.ledger.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE

    @Column(name = "system_role", length = 50)
    private String systemRole;

    @Column(name = "report_group", length = 50)
    private String reportGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    @Column(name = "is_postable", nullable = false)
    private boolean isPostable = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    public Account() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }

    public String getReportGroup() { return reportGroup; }
    public void setReportGroup(String reportGroup) { this.reportGroup = reportGroup; }

    public Account getParent() { return parent; }
    public void setParent(Account parent) { this.parent = parent; }

    public boolean isPostable() { return isPostable; }
    public void setPostable(boolean postable) { isPostable = postable; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
