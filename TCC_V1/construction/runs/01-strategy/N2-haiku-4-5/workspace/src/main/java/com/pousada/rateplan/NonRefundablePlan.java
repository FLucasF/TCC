package com.pousada.rateplan;

import com.pousada.domain.Room;
import com.pousada.utils.Money;
import com.pousada.utils.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundablePlan implements RatePlan {
    private static final BigDecimal DISCOUNT_PERCENT = BigDecimal.valueOf(15);

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException {
        // Non-refundable plan has no special restrictions
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            BigDecimal multiplier = Season.getSeasonMultiplier(date);
            subtotal = Money.add(subtotal, Money.multiply(nightlyRate, multiplier));
        }

        BigDecimal discount = Money.percentage(subtotal, DISCOUNT_PERCENT);
        return new PriceCalculation(subtotal, discount);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
