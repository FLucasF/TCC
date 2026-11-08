package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.service.PricingService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {
    private final PricingService pricingService;

    public StandardRatePlan(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public void validate(Reservation reservation, Room room) {
    }

    @Override
    public BigDecimal calculateSubtotal(Reservation reservation, Room room) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = reservation.getCheckIn();

        while (current.isBefore(reservation.getCheckOut())) {
            BigDecimal nightlyRate = pricingService.calculateNightlyRate(room.getType(), current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return pricingService.round(subtotal);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, Reservation reservation) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
