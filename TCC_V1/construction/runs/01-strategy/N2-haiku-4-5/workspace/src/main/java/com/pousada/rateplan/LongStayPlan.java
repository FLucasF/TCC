package com.pousada.rateplan;

import com.pousada.domain.Room;
import com.pousada.utils.Money;
import com.pousada.utils.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_PERCENT = BigDecimal.valueOf(10);

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < MIN_NIGHTS) {
            throw new RatePlanException("INVALID_REQUEST", "Long stay plan requires at least 7 nights");
        }
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
        return amountPaid;
    }
}
