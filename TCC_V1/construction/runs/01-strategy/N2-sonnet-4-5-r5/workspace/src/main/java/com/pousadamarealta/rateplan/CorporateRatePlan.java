package com.pousadamarealta.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.util.SeasonCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new InvalidRequestException("Company code is required for corporate rate plan");
        }

        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Invalid company code: " + companyCode);
        }
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return calculatePriceWithCompanyCode(room, checkIn, checkOut, null);
    }

    public PricingResult calculatePriceWithCompanyCode(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal companyDiscountRate = COMPANY_DISCOUNTS.getOrDefault(companyCode, BigDecimal.ZERO);

        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();

            if (SeasonCalculator.isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }

            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal discount = subtotal.multiply(companyDiscountRate)
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal total = subtotal.subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
