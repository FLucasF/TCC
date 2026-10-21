package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;

public class LongStayPlan extends RatePlan {
    private static final int MINIMUM_NIGHTS = 7;
    private static final double DISCOUNT_RATE = 0.10;

    @Override
    public String getPlanName() {
        return "LONG_STAY";
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                        int guests, String companyCode) {
        int nights = DateUtils.countNights(checkIn, checkOut);
        if (nights < MINIMUM_NIGHTS) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrices(Room room, LocalDate checkIn, LocalDate checkOut) {
        double subtotal = 0.0;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            double nightlyRate = room.getNightlyRate();
            if (DateUtils.isHighSeason(current)) {
                nightlyRate *= 1.20;
            }
            subtotal += Money.round(nightlyRate);
            current = current.plusDays(1);
        }

        subtotal = Money.round(subtotal);
        double discount = Money.round(subtotal * DISCOUNT_RATE);
        double total = Money.round(subtotal - discount);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public double calculateRefund(double amountPaid) {
        return amountPaid;
    }
}
