package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {

    @Override
    public void validate(Reservation reservation, Room room) {
    }

    @Override
    public void calculatePricing(Reservation reservation, Room room) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = reservation.getCheckIn();

        while (date.isBefore(reservation.getCheckOut())) {
            BigDecimal nightRate = room.getNightlyRate();
            if (isHighSeason(date)) {
                nightRate = nightRate.multiply(new BigDecimal("1.20"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);

        reservation.setSubtotal(subtotal);
        reservation.setDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        reservation.setTotal(subtotal);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }

    protected boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
