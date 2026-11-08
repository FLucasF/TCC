package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.service.PricingService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlanStrategy {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    private final PricingService pricingService;

    public WeekendPackageRatePlan(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Override
    public void validate(Reservation reservation, Room room) {
        long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        if (nights != 2) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package requires exactly 2 nights");
        }

        DayOfWeek checkInDay = reservation.getCheckIn().getDayOfWeek();
        DayOfWeek checkOutDay = reservation.getCheckOut().getDayOfWeek();

        if (checkInDay != DayOfWeek.FRIDAY || checkOutDay != DayOfWeek.SUNDAY) {
            throw new ApiException(400, "INVALID_REQUEST", "Weekend package must be Friday to Sunday");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Reservation reservation, Room room) {
        return PACKAGE_PRICES.get(room.getType());
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, Reservation reservation) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return pricingService.round(reservation.getAmountPaid().multiply(new BigDecimal("0.5")));
    }
}
