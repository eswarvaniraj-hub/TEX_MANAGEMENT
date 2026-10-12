package com.textile.accounts.masterdata.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.dto.MasterDataDtos.*;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.masterdata.repository.CustomerRepository;
import com.textile.accounts.masterdata.repository.GstStateRepository;
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
public class CustomerService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "city", "active", "id");

    private final CustomerRepository customerRepository;
    private final GstStateRepository gstStateRepository;
    private final AuditService auditService;

    public CustomerService(
            CustomerRepository customerRepository,
            GstStateRepository gstStateRepository,
            AuditService auditService
    ) {
        this.customerRepository = customerRepository;
        this.gstStateRepository = gstStateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchCustomers(String q, Boolean active, Pageable pageable) {
        return customerRepository.searchCustomers(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return getCustomerById(id);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(String q, Boolean active, int page, int size, String sortField, String sortDir) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);

        String validSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(validSort).descending() : Sort.by(validSort).ascending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);

        return customerRepository.searchCustomers(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request, String actor) {
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
            code = generateNextCustomerCode();
        } else {
            code = code.trim().toUpperCase();
            if (customerRepository.existsByCodeIgnoreCase(code)) {
                throw new IllegalArgumentException("Customer code already exists: " + code);
            }
        }

        Customer customer = new Customer();
        customer.setCode(code);
        customer.setName(request.name().trim());
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(state);
        customer.setGstin(normalizedGstin);
        customer.setPan(normalizedPan);
        customer.setNotes(request.notes());
        customer.setActive(request.active() == null || request.active());
        customer.setCreatedAt(Instant.now());
        customer.setUpdatedAt(Instant.now());

        Customer saved = customerRepository.save(customer);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "CUSTOMER",
                saved.getId().toString(),
                "Created customer: " + saved.getName() + " (" + saved.getCode() + ")",
                Map.of("code", saved.getCode(), "name", saved.getName(), "state", saved.getState().getCode())
        );

        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request, Long submittedVersion, String actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(customer.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Customer was modified by another transaction.");
        }

        String stateCode = request.stateCode() != null ? request.stateCode().trim() : customer.getState().getCode();
        GstState state = gstStateRepository.findById(stateCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid GST State code: " + stateCode));

        String normalizedGstin = MasterDataValidationUtils.validateAndNormalizeGstin(request.gstin(), stateCode);
        String normalizedPan = MasterDataValidationUtils.validateAndNormalizePan(request.pan());

        customer.setName(request.name().trim());
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(state);
        customer.setGstin(normalizedGstin);
        customer.setPan(normalizedPan);
        customer.setNotes(request.notes());
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        customer.setUpdatedAt(Instant.now());

        Customer updated = customerRepository.save(customer);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "CUSTOMER",
                updated.getId().toString(),
                "Updated customer: " + updated.getName() + " (" + updated.getCode() + ")",
                Map.of("code", updated.getCode(), "name", updated.getName())
        );

        return toResponse(updated);
    }

    @Transactional
    public CustomerResponse setCustomerActive(Long id, boolean active, String actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        customer.setActive(active);
        customer.setUpdatedAt(Instant.now());
        Customer updated = customerRepository.save(customer);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "CUSTOMER",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " customer: " + updated.getCode(),
                Map.of("code", updated.getCode(), "active", active)
        );

        return toResponse(updated);
    }

    private synchronized String generateNextCustomerCode() {
        Integer maxSuffix = customerRepository.findMaxCustomerNumericSuffix();
        int nextNum = (maxSuffix != null ? maxSuffix : 0) + 1;
        while (customerRepository.existsByCodeIgnoreCase(String.format("C%04d", nextNum))) {
            nextNum++;
        }
        return String.format("C%04d", nextNum);
    }

    public CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getContactPerson(),
                c.getPhone(),
                c.getEmail(),
                c.getAddress(),
                c.getCity(),
                c.getState().getCode(),
                c.getState().getName(),
                c.getGstin(),
                c.getPan(),
                c.getNotes(),
                c.isActive(),
                c.getVersion()
        );
    }
}
