package com.pousada.rateplan;

import com.pousada.domain.Room;
import com.pousada.utils.Money;
import com.pousada.utils.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardPlan implements RatePlan {
    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException {
        // Standard plan has no special restrictions
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            BigDecimal multiplier = Season.getSeasonMultiplier(date);
            subtotal = Money.add(subtotal, Money.multiply(nightlyRate, multiplier));
        }

        return new PriceCalculation(subtotal, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
