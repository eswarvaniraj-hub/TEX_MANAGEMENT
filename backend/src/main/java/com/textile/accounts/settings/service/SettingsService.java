package com.textile.accounts.settings.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.service.MasterDataValidationUtils;
import com.textile.accounts.settings.dto.SettingsDtos.BusinessProfileResponse;
import com.textile.accounts.settings.dto.SettingsDtos.BusinessProfileUpdateRequest;
import com.textile.accounts.settings.dto.SettingsDtos.LockDateUpdateRequest;
import com.textile.accounts.settings.model.BusinessProfile;
import com.textile.accounts.settings.repository.BusinessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
public class SettingsService {

    private final BusinessProfileRepository businessProfileRepository;
    private final AuditService auditService;

    public SettingsService(BusinessProfileRepository businessProfileRepository, AuditService auditService) {
        this.businessProfileRepository = businessProfileRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public BusinessProfile getProfileEntity() {
        return businessProfileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Business Profile not initialized"));
    }

    @Transactional(readOnly = true)
    public BusinessProfileResponse getProfile() {
        return toResponse(getProfileEntity());
    }

    @Transactional
    public BusinessProfileResponse updateProfile(BusinessProfileUpdateRequest request, String actor) {
        BusinessProfile profile = getProfileEntity();

        if (request.version() != null && !Objects.equals(profile.getVersion(), request.version())) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Profile was updated by another transaction.");
        }

        String normalizedGstin = MasterDataValidationUtils.validateAndNormalizeGstin(request.gstin(), request.stateCode());
        String normalizedPan = MasterDataValidationUtils.validateAndNormalizePan(request.pan());

        profile.setLegalName(request.legalName().trim());
        profile.setTradeName(request.tradeName().trim());
        profile.setAddress(request.address().trim());
        profile.setStateCode(request.stateCode().trim());
        profile.setGstin(normalizedGstin);
        profile.setPan(normalizedPan);
        profile.setPhone(request.phone().trim());
        profile.setEmail(request.email().trim());
        profile.setInvoiceFooter(request.invoiceFooter());
        profile.setBankDetailsText(request.bankDetailsText());
        if (request.gstRegistered() != null) {
            profile.setGstRegistered(request.gstRegistered());
        }
        if (request.roundInvoiceTotal() != null) {
            profile.setRoundInvoiceTotal(request.roundInvoiceTotal());
        }
        profile.setUpdatedAt(Instant.now());

        BusinessProfile saved = businessProfileRepository.save(profile);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "BUSINESS_PROFILE",
                saved.getId().toString(),
                "Updated Business Profile: " + saved.getTradeName(),
                Map.of("tradeName", saved.getTradeName(), "stateCode", saved.getStateCode())
        );

        return toResponse(saved);
    }

    @Transactional
    public BusinessProfileResponse updateLockDate(LockDateUpdateRequest request, String actor) {
        BusinessProfile profile = getProfileEntity();

        if (request.version() != null && !Objects.equals(profile.getVersion(), request.version())) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Profile was updated by another transaction.");
        }

        profile.setLockDate(request.lockDate());
        profile.setUpdatedAt(Instant.now());

        BusinessProfile saved = businessProfileRepository.save(profile);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE_LOCK_DATE",
                "BUSINESS_PROFILE",
                saved.getId().toString(),
                "Updated Books Lock Date to: " + (request.lockDate() != null ? request.lockDate().toString() : "None"),
                Map.of("lockDate", request.lockDate() != null ? request.lockDate().toString() : "None")
        );

        return toResponse(saved);
    }

    public BusinessProfileResponse toResponse(BusinessProfile p) {
        return new BusinessProfileResponse(
                p.getId(),
                p.getLegalName(),
                p.getTradeName(),
                p.getAddress(),
                p.getStateCode(),
                p.getGstin(),
                p.getPan(),
                p.getPhone(),
                p.getEmail(),
                p.getInvoiceFooter(),
                p.getBankDetailsText(),
                p.isGstRegistered(),
                p.isRoundInvoiceTotal(),
                p.getLockDate(),
                p.getVersion()
        );
    }
}
