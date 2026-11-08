package br.com.maralta.domain.rateplan;

import br.com.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType, String companyCode) {
        if (companyCode == null || companyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Company code is required for corporate rate plan");
        }

        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Invalid company code: " + companyCode);
        }
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType) {
        BigDecimal baseRate = roomType.getNightlyRate();
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate currentDate = checkIn;
        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = baseRate;
            if (isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"));
            }
            nightRate = nightRate.setScale(2, RoundingMode.HALF_EVEN);
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, String companyCode) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        if (discountRate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = subtotal.multiply(discountRate);
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
