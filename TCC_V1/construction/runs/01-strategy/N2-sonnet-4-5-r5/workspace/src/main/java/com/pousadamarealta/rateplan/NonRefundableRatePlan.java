package com.pousadamarealta.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.util.SeasonCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class NonRefundableRatePlan implements RatePlanStrategy {

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
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

        BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"))
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal total = subtotal.subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
