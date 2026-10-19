package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.ValidationResult;
import com.pousada.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends StandardRatePlan {

    private static final Map<String, Integer> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", 12,
        "AGROVALE", 18,
        "FARMAVIDA", 25
    );

    @Override
    public ValidationResult validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            return ValidationResult.invalid("Company code is required for corporate rate plan");
        }

        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            return ValidationResult.invalid("Invalid company code: " + companyCode);
        }

        return ValidationResult.valid();
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        return calculatePriceWithCompany(room, checkIn, nights, null);
    }

    public PriceCalculation calculatePriceWithCompany(Room room, LocalDate checkIn, int nights, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, nights);

        int discountPercent = COMPANY_DISCOUNTS.getOrDefault(companyCode, 0);
        BigDecimal discount = MoneyUtils.percentage(subtotal, discountPercent);
        BigDecimal total = MoneyUtils.roundMoney(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
