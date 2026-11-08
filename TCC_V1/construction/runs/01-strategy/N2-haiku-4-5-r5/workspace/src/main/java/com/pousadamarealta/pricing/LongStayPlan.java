package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.api.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayPlan implements RatePlan {
    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 7) {
            throw new ApiException(400, "INVALID_REQUEST", "Long stay plan requires at least 7 nights");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = room.nightlyRate();

            if (Season.isHighSeason(current)) {
                nightPrice = nightPrice.multiply(BigDecimal.valueOf(1.20));
            }

            nightPrice = MoneyRounder.round(nightPrice);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        subtotal = MoneyRounder.round(subtotal);
        BigDecimal discount = MoneyRounder.round(subtotal.multiply(BigDecimal.valueOf(0.10)));
        BigDecimal total = MoneyRounder.round(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
