package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;

public class NonRefundablePlan extends RatePlan {
    private static final double DISCOUNT_RATE = 0.15;

    @Override
    public String getPlanName() {
        return "NON_REFUNDABLE";
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                        int guests, String companyCode) {
        // No special validations
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
        return 0.0;
    }
}
