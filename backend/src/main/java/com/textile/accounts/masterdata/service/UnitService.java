package com.textile.accounts.masterdata.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.dto.MasterDataDtos.*;
import com.textile.accounts.masterdata.model.Unit;
import com.textile.accounts.masterdata.repository.UnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UnitService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "decimalPlaces", "active", "id");

    private final UnitRepository unitRepository;
    private final AuditService auditService;

    public UnitService(UnitRepository unitRepository, AuditService auditService) {
        this.unitRepository = unitRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> getAllUnits(Boolean active) {
        List<Unit> units = (active == null)
                ? unitRepository.findAllByOrderByCodeAsc()
                : unitRepository.findAllByActive(active);
        return units.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UnitResponse getById(Long id) {
        return getUnitById(id);
    }

    @Transactional(readOnly = true)
    public Page<UnitResponse> listUnits(String q, Boolean active, int page, int size, String sortField, String sortDir) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);

        String validSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "code";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(validSort).descending() : Sort.by(validSort).ascending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);

        return unitRepository.searchUnits(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnitById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with ID: " + id));
        return toResponse(unit);
    }

    @Transactional
    public UnitResponse createUnit(UnitRequest request, String actor) {
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Unit code is required.");
        }
        String cleanCode = request.code().trim().toLowerCase();
        if (unitRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new IllegalArgumentException("Unit code already exists: " + cleanCode);
        }

        if (request.decimalPlaces() < 0 || request.decimalPlaces() > 3) {
            throw new IllegalArgumentException("Unit decimal places must be between 0 and 3.");
        }

        Unit unit = new Unit();
        unit.setCode(cleanCode);
        unit.setName(request.name().trim());
        unit.setDecimalPlaces(request.decimalPlaces());
        unit.setActive(request.active() == null || request.active());
        unit.setCreatedAt(Instant.now());
        unit.setUpdatedAt(Instant.now());

        Unit saved = unitRepository.save(unit);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "UNIT",
                saved.getId().toString(),
                "Created unit: " + saved.getCode(),
                Map.of("code", saved.getCode(), "decimalPlaces", saved.getDecimalPlaces())
        );

        return toResponse(saved);
    }

    @Transactional
    public UnitResponse updateUnit(Long id, UnitRequest request, Long submittedVersion, String actor) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(unit.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Unit was modified by another transaction.");
        }

        // Rule: decimal_places may never be decreased
        if (request.decimalPlaces() < unit.getDecimalPlaces()) {
            throw new IllegalArgumentException(String.format(
                    "Unit decimal places cannot be decreased from %d to %d.",
                    unit.getDecimalPlaces(), request.decimalPlaces()
            ));
        }

        unit.setName(request.name().trim());
        unit.setDecimalPlaces(request.decimalPlaces());
        if (request.active() != null) {
            unit.setActive(request.active());
        }
        unit.setUpdatedAt(Instant.now());

        Unit updated = unitRepository.save(unit);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "UNIT",
                updated.getId().toString(),
                "Updated unit: " + updated.getCode(),
                Map.of("code", updated.getCode(), "decimalPlaces", updated.getDecimalPlaces())
        );

        return toResponse(updated);
    }

    @Transactional
    public UnitResponse setUnitActive(Long id, boolean active, String actor) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with ID: " + id));
        unit.setActive(active);
        unit.setUpdatedAt(Instant.now());
        Unit updated = unitRepository.save(unit);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "UNIT",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " unit: " + updated.getCode(),
                Map.of("code", updated.getCode(), "active", active)
        );

        return toResponse(updated);
    }

    /**
     * Validates whether a quantity value conforms to the unit's permitted decimal precision.
     */
    public static void validateQuantityPrecision(BigDecimal quantity, Unit unit) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        int maxDecimals = unit.getDecimalPlaces();
        BigDecimal stripped = quantity.stripTrailingZeros();
        int actualScale = Math.max(0, stripped.scale());
        if (actualScale > maxDecimals) {
            throw new IllegalArgumentException(String.format(
                    "Quantity %s exceeds maximum permitted decimal places (%d) for unit '%s'.",
                    quantity.toPlainString(), maxDecimals, unit.getCode()
            ));
        }
    }

    public UnitResponse toResponse(Unit u) {
        return new UnitResponse(
                u.getId(),
                u.getCode(),
                u.getName(),
                u.getDecimalPlaces(),
                u.isActive(),
                u.getVersion()
        );
    }
}
