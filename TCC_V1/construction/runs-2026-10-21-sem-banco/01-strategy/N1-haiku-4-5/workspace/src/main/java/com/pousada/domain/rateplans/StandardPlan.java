package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;

public class StandardPlan extends RatePlan {
    @Override
    public String getPlanName() {
        return "STANDARD";
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                        int guests, String companyCode) {
        // No special validations for standard plan
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
        double discount = 0.0;
        double total = Money.round(subtotal - discount);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public double calculateRefund(double amountPaid) {
        return amountPaid;
    }
}
