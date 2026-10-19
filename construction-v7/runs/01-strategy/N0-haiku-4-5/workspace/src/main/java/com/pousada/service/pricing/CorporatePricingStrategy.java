package com.pousada.service.pricing;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import com.pousada.util.DateHelper;
import com.pousada.util.MoneyRounder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CorporatePricingStrategy implements PricingStrategy {
    private static final Map<String, Integer> COMPANY_DISCOUNTS = new HashMap<>();

    static {
        COMPANY_DISCOUNTS.put("TECNOSUL", 12);
        COMPANY_DISCOUNTS.put("AGROVALE", 18);
        COMPANY_DISCOUNTS.put("FARMAVIDA", 25);
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) throws ApiException {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid or missing company code for CORPORATE rate plan");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = room.getNightlyRate();

            if (DateHelper.isHighSeason(current)) {
                nightPrice = MoneyRounder.multiply(nightPrice, BigDecimal.valueOf(1.2));
            }

            subtotal = MoneyRounder.add(subtotal, nightPrice);
            current = current.plusDays(1);
        }

        int discountPercent = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = MoneyRounder.percentage(subtotal, discountPercent);
        BigDecimal total = MoneyRounder.subtract(subtotal, discount);

        return new PricingResult(subtotal, discount, total, BigDecimal.valueOf(100));
    }
}
