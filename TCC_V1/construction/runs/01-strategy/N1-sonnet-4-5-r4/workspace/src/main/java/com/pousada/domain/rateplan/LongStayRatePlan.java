package com.pousada.domain.rateplan;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends AbstractRatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = calculateNights(checkIn, checkOut);

        if (nights < 7) {
            throw new IllegalArgumentException("Estadia longa requer no mínimo 7 noites");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = checkIn;

        while (currentNight.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(nightlyRate, currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }

        return roundMoney(subtotal);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal discount = subtotal.multiply(new BigDecimal("0.10"));
        return roundMoney(discount);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
