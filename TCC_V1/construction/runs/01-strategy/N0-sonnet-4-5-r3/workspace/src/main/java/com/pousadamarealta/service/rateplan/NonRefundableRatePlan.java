package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NonRefundableRatePlan extends StandardRatePlan {

    @Override
    public void calculatePricing(Reservation reservation, com.pousadamarealta.domain.Room room) {
        super.calculatePricing(reservation, room);

        BigDecimal discount = reservation.getSubtotal()
            .multiply(new BigDecimal("0.15"))
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal total = reservation.getSubtotal()
            .subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        reservation.setDiscount(discount);
        reservation.setTotal(total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return BigDecimal.ZERO;
    }
}
