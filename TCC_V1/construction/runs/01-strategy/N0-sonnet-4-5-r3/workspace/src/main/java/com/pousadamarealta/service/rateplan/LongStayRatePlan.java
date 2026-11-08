package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LongStayRatePlan extends StandardRatePlan {

    @Override
    public void validate(Reservation reservation, Room room) {
        if (reservation.getNights() < 7) {
            throw new ApiException(400, "INVALID_REQUEST", "Long stay requires at least 7 nights");
        }
    }

    @Override
    public void calculatePricing(Reservation reservation, Room room) {
        super.calculatePricing(reservation, room);

        BigDecimal discount = reservation.getSubtotal()
            .multiply(new BigDecimal("0.10"))
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal total = reservation.getSubtotal()
            .subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        reservation.setDiscount(discount);
        reservation.setTotal(total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
