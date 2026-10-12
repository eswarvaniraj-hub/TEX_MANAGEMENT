package com.textile.accounts.masterdata.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.dto.MasterDataDtos.TaxRateRequest;
import com.textile.accounts.masterdata.dto.MasterDataDtos.TaxRateResponse;
import com.textile.accounts.masterdata.model.TaxRate;
import com.textile.accounts.masterdata.repository.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;
    private final AuditService auditService;

    public TaxRateService(TaxRateRepository taxRateRepository, AuditService auditService) {
        this.taxRateRepository = taxRateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TaxRateResponse> getAllTaxRates(Boolean active) {
        List<TaxRate> list = (active == null)
                ? taxRateRepository.findAll()
                : taxRateRepository.findAllByActiveOrderByRatePercentAsc(active);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaxRateResponse getById(Long id) {
        return taxRateRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Tax Rate not found with ID: " + id));
    }

    @Transactional
    public TaxRateResponse createTaxRate(TaxRateRequest request, String actor) {
        TaxRate taxRate = new TaxRate();
        taxRate.setName(request.name().trim());
        taxRate.setRatePercent(request.ratePercent());
        taxRate.setActive(request.active() != null ? request.active() : true);
        taxRate.setCreatedAt(Instant.now());
        taxRate.setUpdatedAt(Instant.now());

        TaxRate saved = taxRateRepository.save(taxRate);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "TAX_RATE",
                saved.getId().toString(),
                "Created Tax Rate: " + saved.getName() + " (" + saved.getRatePercent() + "%)",
                Map.of("name", saved.getName(), "ratePercent", saved.getRatePercent())
        );

        return toResponse(saved);
    }

    @Transactional
    public TaxRateResponse updateTaxRate(Long id, TaxRateRequest request, Long submittedVersion, String actor) {
        TaxRate taxRate = taxRateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax Rate not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(taxRate.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Tax Rate was modified by another transaction.");
        }

        taxRate.setName(request.name().trim());
        taxRate.setRatePercent(request.ratePercent());
        if (request.active() != null) {
            taxRate.setActive(request.active());
        }
        taxRate.setUpdatedAt(Instant.now());

        TaxRate updated = taxRateRepository.save(taxRate);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "TAX_RATE",
                updated.getId().toString(),
                "Updated Tax Rate: " + updated.getName() + " (" + updated.getRatePercent() + "%)",
                Map.of("name", updated.getName(), "ratePercent", updated.getRatePercent())
        );

        return toResponse(updated);
    }

    @Transactional
    public TaxRateResponse setTaxRateActive(Long id, boolean active, String actor) {
        TaxRate taxRate = taxRateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax Rate not found with ID: " + id));
        taxRate.setActive(active);
        taxRate.setUpdatedAt(Instant.now());
        TaxRate updated = taxRateRepository.save(taxRate);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "TAX_RATE",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " Tax Rate: " + updated.getName(),
                Map.of("active", active)
        );

        return toResponse(updated);
    }

    public TaxRateResponse toResponse(TaxRate r) {
        return new TaxRateResponse(
                r.getId(),
                r.getName(),
                r.getRatePercent(),
                r.isActive(),
                r.getVersion()
        );
    }
}
