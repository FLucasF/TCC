package com.pousadamarealta.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.util.SeasonCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {

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

        return new PricingResult(subtotal, BigDecimal.ZERO, subtotal);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
