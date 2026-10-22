package com.pousadamareaalta.pricing;

import com.pousadamareaalta.exception.InvalidRequestException;
import com.pousadamareaalta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends StandardRatePlan {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights < MINIMUM_NIGHTS) {
            throw new InvalidRequestException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(roomType, checkIn, nights);
        BigDecimal discount = SeasonUtil.round(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = SeasonUtil.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
