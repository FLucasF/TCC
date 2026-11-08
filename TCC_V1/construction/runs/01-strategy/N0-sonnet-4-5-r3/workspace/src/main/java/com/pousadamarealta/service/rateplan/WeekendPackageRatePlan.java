package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(Reservation reservation, Room room) {
        if (reservation.getNights() != 2) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package requires exactly 2 nights");
        }

        if (reservation.getCheckIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must start on Friday");
        }

        if (reservation.getCheckOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must end on Sunday");
        }
    }

    @Override
    public void calculatePricing(Reservation reservation, Room room) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());

        reservation.setSubtotal(packagePrice);
        reservation.setDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        reservation.setTotal(packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid()
            .multiply(new BigDecimal("0.50"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
