package com.textile.accounts.sales.service;

import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.Product;
import com.textile.accounts.masterdata.model.Unit;
import com.textile.accounts.masterdata.repository.CustomerRepository;
import com.textile.accounts.masterdata.repository.ProductRepository;
import com.textile.accounts.masterdata.repository.UnitRepository;
import com.textile.accounts.sales.dto.SalesDtos.CalculatedLineResponse;
import com.textile.accounts.sales.dto.SalesDtos.CalculationResponse;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceLineRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceRequest;
import com.textile.accounts.settings.model.BusinessProfile;
import com.textile.accounts.settings.service.SettingsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesInvoiceCalculationService {

    private final SettingsService settingsService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;

    public SalesInvoiceCalculationService(
            SettingsService settingsService,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            UnitRepository unitRepository
    ) {
        this.settingsService = settingsService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
    }

    public CalculationResponse calculate(SalesInvoiceRequest request) {
        BusinessProfile profile = settingsService.getProfileEntity();
        String sellerStateCode = profile.getStateCode();

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        String placeOfSupplyStateCode = request.placeOfSupplyStateCode() != null && !request.placeOfSupplyStateCode().isBlank()
                ? request.placeOfSupplyStateCode()
                : customer.getState().getCode();

        boolean isInterstate = !sellerStateCode.equalsIgnoreCase(placeOfSupplyStateCode);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;

        List<CalculatedLineResponse> lines = new ArrayList<>();
        int lineNo = 1;

        for (SalesInvoiceLineRequest reqLine : request.lines()) {
            Product product = productRepository.findById(reqLine.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + reqLine.productId()));

            Unit unit = unitRepository.findById(reqLine.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + reqLine.unitId()));

            BigDecimal qty = reqLine.quantity().setScale(unit.getDecimalPlaces(), RoundingMode.HALF_UP);
            BigDecimal rate = reqLine.rate().setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineAmount = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal discount = reqLine.discountAmount() != null
                    ? reqLine.discountAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);

            BigDecimal taxable = lineAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);

            BigDecimal taxRate = reqLine.taxRatePercent() != null
                    ? reqLine.taxRatePercent()
                    : (product.getDefaultTaxRate() != null ? product.getDefaultTaxRate().getRatePercent() : BigDecimal.ZERO);

            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);

            if (isInterstate) {
                igst = taxable.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                BigDecimal halfRate = taxRate.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                cgst = taxable.multiply(halfRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                sgst = taxable.multiply(halfRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }

            BigDecimal lineTotal = taxable.add(cgst).add(sgst).add(igst);

            subtotal = subtotal.add(lineAmount);
            discountTotal = discountTotal.add(discount);
            taxableTotal = taxableTotal.add(taxable);
            cgstTotal = cgstTotal.add(cgst);
            sgstTotal = sgstTotal.add(sgst);
            igstTotal = igstTotal.add(igst);

            lines.add(new CalculatedLineResponse(
                    lineNo++,
                    product.getId(),
                    product.getName(),
                    reqLine.description() != null ? reqLine.description() : product.getDescription(),
                    reqLine.hsnCode() != null ? reqLine.hsnCode() : product.getHsnCode(),
                    qty,
                    unit.getId(),
                    unit.getCode(),
                    rate,
                    lineAmount,
                    discount,
                    taxable,
                    taxRate,
                    cgst,
                    sgst,
                    igst,
                    lineTotal
            ));
        }

        BigDecimal rawGrandTotal = taxableTotal.add(cgstTotal).add(sgstTotal).add(igstTotal);
        BigDecimal roundOff = BigDecimal.ZERO.setScale(2);
        BigDecimal grandTotal = rawGrandTotal;

        if (profile.isRoundInvoiceTotal()) {
            BigDecimal rounded = rawGrandTotal.setScale(0, RoundingMode.HALF_UP).setScale(2);
            roundOff = rounded.subtract(rawGrandTotal);
            grandTotal = rounded;
        }

        return new CalculationResponse(
                isInterstate,
                subtotal,
                discountTotal,
                taxableTotal,
                cgstTotal,
                sgstTotal,
                igstTotal,
                roundOff,
                grandTotal,
                lines
        );
    }
}
