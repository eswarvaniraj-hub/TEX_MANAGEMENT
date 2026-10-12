package com.textile.accounts.masterdata.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.dto.MasterDataDtos.*;
import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.masterdata.model.Supplier;
import com.textile.accounts.masterdata.repository.GstStateRepository;
import com.textile.accounts.masterdata.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SupplierService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "city", "active", "id");

    private final SupplierRepository supplierRepository;
    private final GstStateRepository gstStateRepository;
    private final AuditService auditService;

    public SupplierService(
            SupplierRepository supplierRepository,
            GstStateRepository gstStateRepository,
            AuditService auditService
    ) {
        this.supplierRepository = supplierRepository;
        this.gstStateRepository = gstStateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String q, Boolean active, Pageable pageable) {
        return supplierRepository.searchSuppliers(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return getSupplierById(id);
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> listSuppliers(String q, Boolean active, int page, int size, String sortField, String sortDir) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);

        String validSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(validSort).descending() : Sort.by(validSort).ascending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);

        return supplierRepository.searchSuppliers(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request, String actor) {
        String stateCode = request.stateCode();
        if ((stateCode == null || stateCode.isBlank()) && request.gstin() != null) {
            stateCode = MasterDataValidationUtils.extractStateFromGstin(request.gstin());
        }
        if (stateCode == null || stateCode.isBlank()) {
            throw new IllegalArgumentException("State code is required.");
        }

        String validState = stateCode.trim();
        GstState state = gstStateRepository.findById(validState)
                .orElseThrow(() -> new IllegalArgumentException("Invalid GST State code: " + validState));

        String normalizedGstin = MasterDataValidationUtils.validateAndNormalizeGstin(request.gstin(), validState);
        String normalizedPan = MasterDataValidationUtils.validateAndNormalizePan(request.pan());

        String code = request.code();
        if (code == null || code.isBlank()) {
            code = generateNextSupplierCode();
        } else {
            code = code.trim().toUpperCase();
            if (supplierRepository.existsByCodeIgnoreCase(code)) {
                throw new IllegalArgumentException("Supplier code already exists: " + code);
            }
        }

        Supplier supplier = new Supplier();
        supplier.setCode(code);
        supplier.setName(request.name().trim());
        supplier.setSupplierType(request.supplierType() != null ? request.supplierType().trim() : "YARN");
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(state);
        supplier.setGstin(normalizedGstin);
        supplier.setPan(normalizedPan);
        supplier.setNotes(request.notes());
        supplier.setActive(request.active() == null || request.active());
        supplier.setCreatedAt(Instant.now());
        supplier.setUpdatedAt(Instant.now());

        Supplier saved = supplierRepository.save(supplier);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "SUPPLIER",
                saved.getId().toString(),
                "Created supplier: " + saved.getName() + " (" + saved.getCode() + ")",
                Map.of("code", saved.getCode(), "name", saved.getName(), "state", saved.getState().getCode())
        );

        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request, Long submittedVersion, String actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(supplier.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Supplier was modified by another transaction.");
        }

        String stateCode = request.stateCode() != null ? request.stateCode().trim() : supplier.getState().getCode();
        GstState state = gstStateRepository.findById(stateCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid GST State code: " + stateCode));

        String normalizedGstin = MasterDataValidationUtils.validateAndNormalizeGstin(request.gstin(), stateCode);
        String normalizedPan = MasterDataValidationUtils.validateAndNormalizePan(request.pan());

        supplier.setName(request.name().trim());
        if (request.supplierType() != null) {
            supplier.setSupplierType(request.supplierType().trim());
        }
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(state);
        supplier.setGstin(normalizedGstin);
        supplier.setPan(normalizedPan);
        supplier.setNotes(request.notes());
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
        supplier.setUpdatedAt(Instant.now());

        Supplier updated = supplierRepository.save(supplier);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "SUPPLIER",
                updated.getId().toString(),
                "Updated supplier: " + updated.getName() + " (" + updated.getCode() + ")",
                Map.of("code", updated.getCode(), "name", updated.getName())
        );

        return toResponse(updated);
    }

    @Transactional
    public SupplierResponse setSupplierActive(Long id, boolean active, String actor) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
        supplier.setActive(active);
        supplier.setUpdatedAt(Instant.now());
        Supplier updated = supplierRepository.save(supplier);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "SUPPLIER",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " supplier: " + updated.getCode(),
                Map.of("code", updated.getCode(), "active", active)
        );

        return toResponse(updated);
    }

    private synchronized String generateNextSupplierCode() {
        int nextNum = 1;
        while (supplierRepository.existsByCodeIgnoreCase(String.format("S%04d", nextNum))) {
            nextNum++;
        }
        return String.format("S%04d", nextNum);
    }

    public SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getSupplierType(),
                s.getContactPerson(),
                s.getPhone(),
                s.getEmail(),
                s.getAddress(),
                s.getCity(),
                s.getState().getCode(),
                s.getState().getName(),
                s.getGstin(),
                s.getPan(),
                s.getNotes(),
                s.isActive(),
                s.getVersion()
        );
    }
}
