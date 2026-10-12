package com.textile.accounts.masterdata.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.masterdata.dto.MasterDataDtos.*;
import com.textile.accounts.masterdata.model.Product;
import com.textile.accounts.masterdata.model.TaxRate;
import com.textile.accounts.masterdata.model.Unit;
import com.textile.accounts.masterdata.repository.ProductRepository;
import com.textile.accounts.masterdata.repository.TaxRateRepository;
import com.textile.accounts.masterdata.repository.UnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "productType", "active", "id");

    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final TaxRateRepository taxRateRepository;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            UnitRepository unitRepository,
            TaxRateRepository taxRateRepository,
            AuditService auditService
    ) {
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.taxRateRepository = taxRateRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String q, Boolean active, String productType, Pageable pageable) {
        return productRepository.searchProducts(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return getProductById(id);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String q, Boolean active, int page, int size, String sortField, String sortDir) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);

        String validSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(validSort).descending() : Sort.by(validSort).ascending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);

        return productRepository.searchProducts(q, active, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, String actor) {
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with ID: " + request.unitId()));

        TaxRate taxRate = null;
        if (request.defaultTaxRateId() != null) {
            taxRate = taxRateRepository.findById(request.defaultTaxRateId())
                    .orElseThrow(() -> new IllegalArgumentException("Tax rate not found with ID: " + request.defaultTaxRateId()));
        }

        String normalizedHsn = MasterDataValidationUtils.validateAndNormalizeHsn(request.hsnCode());

        String code = request.code();
        if (code == null || code.isBlank()) {
            code = generateNextProductCode();
        } else {
            code = code.trim().toUpperCase();
            if (productRepository.existsByCodeIgnoreCase(code)) {
                throw new IllegalArgumentException("Product code already exists: " + code);
            }
        }

        BigDecimal saleRate = request.defaultSaleRate() != null ? request.defaultSaleRate() : BigDecimal.ZERO;
        BigDecimal purchaseRate = request.defaultPurchaseRate() != null ? request.defaultPurchaseRate() : BigDecimal.ZERO;
        if (saleRate.compareTo(BigDecimal.ZERO) < 0 || purchaseRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rates must be greater than or equal to 0.");
        }

        Product product = new Product();
        product.setCode(code);
        product.setName(request.name().trim());
        product.setProductType(request.productType().trim());
        product.setUnit(unit);
        product.setHsnCode(normalizedHsn);
        product.setDefaultTaxRate(taxRate);
        product.setDefaultSaleRate(saleRate);
        product.setDefaultPurchaseRate(purchaseRate);
        product.setDescription(request.description());
        product.setActive(request.active() == null || request.active());
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());

        Product saved = productRepository.save(product);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "PRODUCT",
                saved.getId().toString(),
                "Created product: " + saved.getName() + " (" + saved.getCode() + ")",
                Map.of("code", saved.getCode(), "name", saved.getName())
        );

        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long submittedVersion, String actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(product.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Product was modified by another transaction.");
        }

        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with ID: " + request.unitId()));

        TaxRate taxRate = null;
        if (request.defaultTaxRateId() != null) {
            taxRate = taxRateRepository.findById(request.defaultTaxRateId())
                    .orElseThrow(() -> new IllegalArgumentException("Tax rate not found with ID: " + request.defaultTaxRateId()));
        }

        String normalizedHsn = MasterDataValidationUtils.validateAndNormalizeHsn(request.hsnCode());

        BigDecimal saleRate = request.defaultSaleRate() != null ? request.defaultSaleRate() : product.getDefaultSaleRate();
        BigDecimal purchaseRate = request.defaultPurchaseRate() != null ? request.defaultPurchaseRate() : product.getDefaultPurchaseRate();
        if (saleRate.compareTo(BigDecimal.ZERO) < 0 || purchaseRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rates must be greater than or equal to 0.");
        }

        product.setName(request.name().trim());
        product.setProductType(request.productType().trim());
        product.setUnit(unit);
        product.setHsnCode(normalizedHsn);
        product.setDefaultTaxRate(taxRate);
        product.setDefaultSaleRate(saleRate);
        product.setDefaultPurchaseRate(purchaseRate);
        product.setDescription(request.description());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        product.setUpdatedAt(Instant.now());

        Product updated = productRepository.save(product);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "PRODUCT",
                updated.getId().toString(),
                "Updated product: " + updated.getName() + " (" + updated.getCode() + ")",
                Map.of("code", updated.getCode(), "name", updated.getName())
        );

        return toResponse(updated);
    }

    @Transactional
    public ProductResponse setProductActive(Long id, boolean active, String actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        product.setActive(active);
        product.setUpdatedAt(Instant.now());
        Product updated = productRepository.save(product);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "PRODUCT",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " product: " + updated.getCode(),
                Map.of("code", updated.getCode(), "active", active)
        );

        return toResponse(updated);
    }

    private synchronized String generateNextProductCode() {
        int nextNum = 1;
        while (productRepository.existsByCodeIgnoreCase(String.format("P%04d", nextNum))) {
            nextNum++;
        }
        return String.format("P%04d", nextNum);
    }

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getProductType(),
                p.getUnit().getId(),
                p.getUnit().getCode(),
                p.getUnit().getDecimalPlaces(),
                p.getHsnCode(),
                p.getDefaultTaxRate() != null ? p.getDefaultTaxRate().getId() : null,
                p.getDefaultTaxRate() != null ? p.getDefaultTaxRate().getRatePercent() : null,
                p.getDefaultSaleRate(),
                p.getDefaultPurchaseRate(),
                p.getDescription(),
                p.isActive(),
                p.getVersion()
        );
    }
}
