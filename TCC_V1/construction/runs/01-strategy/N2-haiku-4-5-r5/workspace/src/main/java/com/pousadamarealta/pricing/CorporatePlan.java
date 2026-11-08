package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.api.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
        "TECNOSUL", BigDecimal.valueOf(0.12),
        "AGROVALE", BigDecimal.valueOf(0.18),
        "FARMAVIDA", BigDecimal.valueOf(0.25)
    );

    private String companyCode;

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (companyCode == null || !DISCOUNTS.containsKey(companyCode)) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid company code");
        }

        this.companyCode = companyCode;
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = room.nightlyRate();

            if (Season.isHighSeason(current)) {
                nightPrice = nightPrice.multiply(BigDecimal.valueOf(1.20));
            }

            nightPrice = MoneyRounder.round(nightPrice);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        subtotal = MoneyRounder.round(subtotal);
        BigDecimal discountRate = DISCOUNTS.get(companyCode);
        BigDecimal discount = MoneyRounder.round(subtotal.multiply(discountRate));
        BigDecimal total = MoneyRounder.round(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
