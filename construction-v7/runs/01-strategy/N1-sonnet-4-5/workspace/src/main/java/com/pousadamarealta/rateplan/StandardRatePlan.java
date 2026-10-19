package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {
    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = MoneyUtils.round(BigDecimal.ZERO);
        BigDecimal total = subtotal;

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }

    protected BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.getNightlyRate();

            if (DateUtils.isHighSeason(current)) {
                nightlyRate = MoneyUtils.round(nightlyRate.multiply(new BigDecimal("1.20")));
            } else {
                nightlyRate = MoneyUtils.round(nightlyRate);
            }

            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return MoneyUtils.round(subtotal);
    }
}
